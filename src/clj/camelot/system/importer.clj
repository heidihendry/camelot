(ns camelot.system.importer
  "Importer component."
  (:require
   [com.stuartsierra.component :as component]
   [camelot.bulk-import.import :as import]
   [clojure.core.async :refer [<! chan >! alts! go-loop go close!] :as async]
   [clojure.tools.logging :as log]))

(def queue-buffer-size
  "Maximum buffer size before queue will start dropping."
  500000)

(defn run
  "Importer event loop."
  [config cmd-chan queue-chan]
  (go
    (let [import! (import/import-media-fn
                   (or (get-in config [:config :media-importers]) 1))]
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
                                (deref (get-in (:state msg) [:importer :pending])))
                       (dosync
                        (ref-set (get-in (:state msg) [:importer :complete]) 0)
                        (ref-set (get-in (:state msg) [:importer :failed]) 0)))
                nil)

              queue-chan
              (condp = (:type msg)
                :delay
                @(:delay msg)

                :record
                (do
                  (dosync
                   (alter (get-in (:state msg) [:importer :pending]) inc))
                  (import! (:state msg) (:record msg))))))
          (recur))
        (catch InterruptedException e
          (println "Importer stopped."))))))

(defrecord Importer [config]
  component/Lifecycle
  (start [this]
    (if (:cmd-chan this)
      this
      (let [cmd-chan (chan)
            queue-chan (chan (async/dropping-buffer queue-buffer-size))]
        (run config cmd-chan queue-chan)
        (println "Importer started.")
        (assoc this
               :complete (ref 0)
               :failed (ref 0)
               :pending (ref 0)
               :cmd-chan cmd-chan
               :queue-chan queue-chan))))

  (stop [this]
    (when (:cmd-chan this)
      (go (>! (:cmd-chan this) {:cmd :stop})))
    (assoc this
           :complete nil
           :failed nil
           :pending nil
           :cmd-chan nil
           :queue-chan nil)))

(defn importer-state
  "Return the state of the importer."
  [state]
  {:complete @(get-in state [:importer :complete])
   :failed @(get-in state [:importer :failed])
   :pending @(get-in state [:importer :pending])
   :queued (count (.buf (get-in state [:importer :queue-chan])))})
