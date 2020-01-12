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

(def state-serialisation-backoff
  "Delay in ms between attempts to write out the current detector state.
  Idempotence is order-of-the-day; we afford a few seconds of loss."
  (* 15 1000))

(defn- detector-path
  [state]
  (let [file (io/file (-> state :config :paths :database) detector-filename)]
    (.getCanonicalPath ^File file)))

(defn- event-to-analytics
  [v]
  (merge
   {:category "detector"
    :action (name (:action v))
    :label (if-let [s (:subject v)] (name s))
    :label-value (let [lv (:subject-id v)] (when (int? lv) lv))
    :ni true}
   (:meta v)))

(defn- command-to-analytics
  [v]
  (merge
   {:category "detector"
    :action (str (name (:cmd v)) "-command")
    :ni (= (:set-by v) :user)}
   (:meta v)))

(defn- command-to-status
  [cmd]
  (condp = cmd
    :stop :stopped
    :pause :paused
    :resume :running))

(defn- event-to-system-status
  [evt]
  {:status (command-to-status (:cmd evt))
   :set-by (:set-by evt)})

(defrecord Detector [config database]
  component/Lifecycle
  (start [this]
    (let [state {:config config :database database}]
      (if (-> state :config :detector :enabled)
        (if (:cmd-chan this)
          this
          (do
            (analytics/track state {:category "detector"
                                    :action "startup"
                                    :ni true})
            (log/info "Starting detector...")
            (let [latest-state (async/chan (async/sliding-buffer 1))
                  detector-state (duratom/duratom :local-file
                                                  :file-path (detector-path state)
                                                  :init {}
                                                  :rw {:read nippy/thaw-from-file
                                                       :write (fn [file data]
                                                                (async/go (async/>! latest-state [file data])))})
                  cmd-chan (async/chan)
                  cmd-mult (async/mult cmd-chan)]

              (let [kf (juxt (constantly :events) :subject :action)
                    int-cmd-ch (async/tap cmd-mult (async/chan))
                    event-chan (detection/run state detector-state cmd-chan cmd-mult)]
                (async/go-loop []
                  (let [[v port] (async/alts! [int-cmd-ch event-chan])]
                    (condp = port
                      int-cmd-ch
                      (do
                        (log/info "Received event to state mgr" v)
                        (condp = (:cmd v)
                          :stop (swap! detector-state assoc :system (event-to-system-status v))
                          :pause (swap! detector-state assoc :system (event-to-system-status v))
                          :resume (swap! detector-state assoc :system (event-to-system-status v))
                          nil)
                        (async/go (async/>! event-chan v))
                        (recur))

                      event-chan
                      (do
                        (cond
                          (= (:subject v) :system-status)
                          (swap! detector-state assoc :system {:status (:action v)})

                          (nil? (:cmd v))
                          (swap! detector-state update-in (kf v) (fnil inc 0)))

                        (log/info "Publishing analytics for " (:action v))
                        (if (:cmd v)
                          (analytics/track state (command-to-analytics v))
                          (analytics/track state (event-to-analytics v)))
                        (recur))))))

              (let [int-cmd-ch (async/tap cmd-mult (async/chan))]
                (async/go-loop []
                  (let [timeout-ch (async/timeout state-serialisation-backoff)
                        [v port] (async/alts! [int-cmd-ch timeout-ch])]
                    (condp = port
                      int-cmd-ch
                      (condp = (:cmd v)
                        :stop
                        (do
                          (if-let [[file data] (async/poll! latest-state)]
                            (nippy/freeze-to-file file data))
                          (swap! detector-state assoc :system {:status :stopped}))

                        (do
                          (log/info "Command received:" v)
                          (recur)))

                      timeout-ch
                      (let [_ (async/<! timeout-ch)]
                        (try
                          (if-let [[file data] (async/poll! latest-state)]
                            (nippy/freeze-to-file file data))
                          (catch Exception e
                            (log/error "Exception while writing state file" e)))
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
