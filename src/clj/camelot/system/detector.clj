(ns camelot.system.detector
  "Wildlife detector component."
  (:require
   [duratom.core :as duratom]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async]
   [camelot.detection.core :as detection]
   [clojure.java.io :as io])
  (:import
   (java.io File)))

(def detector-filename
  "Name of the detector state file."
  "detector.clj")

(defn- detector-path
  [config]
  (let [file (io/file (-> config :config :path :database) detector-filename)]
    (.getCanonicalPath ^File file)))

;; TODO add an endpoint for surfacing the event status
(defrecord Detector [config]
  component/Lifecycle
  (start [this]
    (when (-> config :config :detector :enabled)
      (if (:cmd-chan this)
        this
        (let [detector-state (duratom/duratom :local-file
                                              :file-path (detector-path config)
                                              :init {})
              cmd-chan (async/chan)
              event-chan (detection/run config detector-state cmd-chan)
              events (atom {})]
          (let [kf (juxt :subject :action)]
            (async/go-loop []
              (let [[v port] (async/alts! [cmd-chan event-chan])]
                (condp = port
                  cmd-chan
                  (when-not (= (:cmd v) :stop)
                    (recur))

                  event-chan
                  (do
                    (swap! events update-in (kf v) inc)
                    (recur))))))
          (assoc this
                 :state detector-state
                 :cmd-chan cmd-chan
                 :event-chan event-chan
                 :events events)))))

  (stop [this]
    (when (:cmd-chan this)
      (async/put! (:cmd-chan this) {:cmd :stop}))
    (assoc this
           :state nil
           :cmd-chan nil
           :event-chan nil
           :events nil)))
