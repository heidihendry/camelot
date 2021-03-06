(ns camelot.system.importer
  "Importer component."
  (:require
   [com.stuartsierra.component :as component]
   [camelot.import.bulk :as bulk]
   [clojure.core.async :refer [chan >! alts! go close!] :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]))

(def queue-buffer-size
  "Maximum buffer size before queue will start dropping."
  500000)

(defn run
  "Importer event loop."
  [config cmd-chan queue-chan]
  (go
    (let [import! (bulk/import-media-fn
                   (or (get-in config [:config :server :media-importers]) 1))]
      (try
        (loop []
          (let [[msg ch] (alts! [cmd-chan queue-chan])]
            (condp = ch

              cmd-chan
              (condp = (:cmd msg)
                :stop (do (close! queue-chan)
                          (close! cmd-chan)
                          (throw (InterruptedException.)))
                :new (when (and (zero? (count (.buf queue-chan)))
                                (zero? (deref (get-in (:state msg) [:importer :pending]))))
                       (dosync
                        (ref-set (get-in (:state msg) [:importer :start-time]) (t/now))
                        (ref-set (get-in (:state msg) [:importer :end-time]) nil)
                        (ref-set (get-in (:state msg) [:importer :complete]) 0)
                        (ref-set (get-in (:state msg) [:importer :ignored]) 0)
                        (ref-set (get-in (:state msg) [:importer :failed]) 0)
                        (ref-set (get-in (:state msg) [:importer :failed-paths]) [])))
                :cancel (try
                          (let [b (.buf queue-chan)]
                            (when-not (zero? (count b))
                              (dosync
                               (ref-set (get-in (:state msg) [:importer :ignored]) (count b))
                               (ref-set (get-in (:state msg) [:importer :end-time]) nil)))
                            (loop []
                              (when-not (zero? (count b))
                                (try
                                  (.remove! b)
                                  (catch java.util.NoSuchElementException _
                                    (log/warn "Tried to remove element from buffer, but buffer already empty.")))
                                (recur))))
                          (catch Exception e
                            (log/error (.getMessage ^Exception e))))
                nil)

              queue-chan
              (condp = (:type msg)
                :finish
                (do
                  (dosync
                   (ref-set (get-in (:state msg) [:importer :end-time]) (t/now)))
                  (try
                    (when (:handler msg)
                      (deref (:handler msg)))
                    (catch Exception e
                      (log/error "Finish command handler failed with error: " (.getMessage ^Exception e)))))

                :record
                (do
                  (dosync
                   (alter (get-in (:state msg) [:importer :pending]) inc))
                  (import! (:state msg) (:record msg))))))
          (recur))
        (catch InterruptedException _
          (log/info "Importer stopped."))
        (catch Exception e
          (log/error "Importer failed with error: " (.getMessage ^Exception e)))))))

(defrecord Importer [config]
  component/Lifecycle
  (start [this]
    (if (:cmd-chan this)
      this
      (let [cmd-chan (chan)
            queue-chan (chan (async/dropping-buffer queue-buffer-size))]
        (run config cmd-chan queue-chan)
        (assoc this
               :complete (ref 0)
               :failed (ref 0)
               :ignored (ref 0)
               :pending (ref 0)
               :failed-paths (ref [])
               :start-time (ref nil)
               :end-time (ref nil)
               :cmd-chan cmd-chan
               :queue-chan queue-chan))))

  (stop [this]
    (when (:cmd-chan this)
      (go (>! (:cmd-chan this) {:cmd :stop})))
    (assoc this
           :complete nil
           :failed nil
           :ignored nil
           :pending nil
           :start-time nil
           :end-time nil
           :failed-paths []
           :cmd-chan nil
           :queue-chan nil)))
