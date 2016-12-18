(ns camelot.system.importer
  "Importer component."
  (:require
   [com.stuartsierra.component :as component]
   [clojure.core.async :refer [<! chan >! alts! go-loop go close!] :as async]
   [clojure.tools.logging :as log]))

(def queue-buffer-size
  "Maximum buffer size before queue will start dropping."
  500000)

(defn import!
  "Fake importer."
  [state record]
  (log/info "Importing with capture-timestamp: " (:media-capture-timestamp record))
  (swap! (get-in state [:importer :complete]) inc))

(defn run
  "Importer event loop."
  [cmd-chan queue-chan]
  (try
    (go-loop []
      (let [[msg ch] (alts! [cmd-chan queue-chan])]
        (condp = ch
          cmd-chan (condp = (:cmd msg)
                     :stop (do (close! queue-chan)
                               (close! cmd-chan)
                               (throw (InterruptedException.)))
                     nil)
          queue-chan (import! (:state msg) (:record msg)))
        (recur)))
    (catch InterruptedException e
      (log/info "Importer stopped."))))

(defrecord Importer []
  component/Lifecycle
  (start [this]
    (if (:cmd-chan this)
      this
      (let [cmd-chan (chan)
            queue-chan (chan (async/dropping-buffer queue-buffer-size))]
        (run cmd-chan queue-chan)
        (log/info "Importer started.")
        (assoc this
               :complete (atom 0)
               :failed (atom 0)
               :cmd-chan cmd-chan
               :queue-chan queue-chan))))

  (stop [this]
    (when (:cmd-chan this)
      (go (>! (:cmd-chan this) {:cmd :stop})))
    (assoc this
           :cmd-chan nil
           :queue-chan nil)))

(defn importer-state
  "Return the state of the importer."
  [state]
  {:complete @(get-in state [:importer :complete])
   :failed @(get-in state [:importer :failed])
   :pending (count (.buf (get-in state [:importer :queue-chan])))})
