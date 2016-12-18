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
  (let [import! (import/import-media-fn (or (:media-importers config) 1))]
    (try
      (go-loop []
        (let [[msg ch] (alts! [cmd-chan queue-chan])]
          (condp = ch
            cmd-chan (condp = (:cmd msg)
                       :stop (do (close! queue-chan)
                                 (close! cmd-chan)
                                 (throw (InterruptedException.)))
                       nil)
            queue-chan (do
                         (swap! (get-in (:state msg) [:importer :pending]) inc)
                         (prn (:state msg))
                         (prn (:record msg))
                         (import! (:state msg) (:record msg)))))
        (recur))
      (catch InterruptedException e
        (log/info "Importer stopped.")))))

(defrecord Importer [config]
  component/Lifecycle
  (start [this]
    (if (:cmd-chan this)
      this
      (let [cmd-chan (chan)
            queue-chan (chan (async/dropping-buffer queue-buffer-size))]
        (run config cmd-chan queue-chan)
        (log/info "Importer started.")
        (assoc this
               :complete (atom 0)
               :failed (atom 0)
               :pending (atom 0)
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
