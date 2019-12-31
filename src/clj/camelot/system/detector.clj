(ns camelot.system.detector
  "Wildlife detector component."
  (:require
   [camelot.services.analytics :as analytics]
   [duratom.core :as duratom]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async]
   [camelot.detection.core :as detection]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log])
  (:import
   (java.io File)))

(def detector-filename
  "Name of the detector state file."
  "detector.clj")

(defn- detector-path
  [state]
  (let [file (io/file (-> state :config :path :database) detector-filename)]
    (.getCanonicalPath ^File file)))

(defrecord Detector [config database]
  component/Lifecycle
  (start [this]
    (log/warn config)
    (log/warn database)
    (let [state {:config config :database database}]
      (if (-> state :config :detector :enabled)
        (if (:cmd-chan this)
          this
          (do
            (analytics/track state {:category "detector"
                                    :action "startup"
                                    :ni true})
            (log/info "Starting detector...")
            (let [detector-state (duratom/duratom :local-file
                                                  :file-path (detector-path state)
                                                  :init {})
                  cmd-chan (async/chan)
                  cmd-mult (async/mult cmd-chan)
                  event-chan (detection/run state detector-state cmd-mult)
                  events (atom {})]
              (let [kf (juxt :subject :action)
                    int-cmd-ch (async/chan)]
                (async/tap cmd-mult int-cmd-ch)
                (async/go-loop []
                  (let [[v port] (async/alts! [int-cmd-ch event-chan])]
                    (condp = port
                      int-cmd-ch
                      (when-not (= (:cmd v) :stop)
                        (recur))

                      event-chan
                      (do
                        (swap! events update-in (kf v) (fnil inc 0))
                        (recur))))))
              (assoc this
                     :state detector-state
                     :cmd-chan cmd-chan
                     :event-chan event-chan
                     :events events))))
        this)))

  (stop [this]
    (when (:cmd-chan this)
      (async/put! (:cmd-chan this) {:cmd :stop}))
    (assoc this
           :state nil
           :cmd-chan nil
           :event-chan nil
           :events nil)))
