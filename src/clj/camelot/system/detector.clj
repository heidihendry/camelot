(ns camelot.system.detector
  "Wildlife detector component."
  (:require
   [camelot.services.analytics :as analytics]
   [duratom.core :as duratom]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async]
   [camelot.detection.core :as detection]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [taoensso.nippy :as nippy])
  (:import
   (java.io File)))

(def detector-filename
  "Name of the detector state file."
  "detector.edn")

(defn- detector-path
  [state]
  (let [file (io/file (-> state :config :path :database) detector-filename)]
    (.getCanonicalPath ^File file)))

(defn- event-to-analytics
  [v]
  (merge
   {:category "detector"
    :action (name (:action v))
    :label (name (:subject v))
    :label-value (:subject-id v)
    :ni true}
   (:meta v)))

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
                                                  :init {}
                                                  :rw {:read nippy/thaw-from-file
                                                       :write nippy/freeze-to-file})
                  cmd-chan (async/chan)
                  cmd-mult (async/mult cmd-chan)
                  event-chan (detection/run state detector-state cmd-mult)]
              (let [kf (juxt (constantly :events) :subject :action)
                    int-cmd-ch (async/chan)]
                (async/tap cmd-mult int-cmd-ch)
                (async/go-loop []
                  (let [[v port] (async/alts! [int-cmd-ch event-chan])]
                    (condp = port
                      int-cmd-ch
                      (if (= (:cmd v) :stop)
                        (swap! detector-state assoc-in [:system-status] :offline)
                        (recur))

                      event-chan
                      (do
                        (if (= (:subject v) :system-status)
                          (swap! detector-state assoc :system-status (:action v))
                          (swap! detector-state update-in (kf v) (fnil inc 0)))
                        (analytics/track state (event-to-analytics v))
                        (recur))))))
              (assoc this
                     :state detector-state
                     :cmd-chan cmd-chan))))
        this)))

  (stop [this]
    (when (:cmd-chan this)
      (async/put! (:cmd-chan this) {:cmd :stop}))
    (assoc this
           :state nil
           :cmd-chan nil)))
