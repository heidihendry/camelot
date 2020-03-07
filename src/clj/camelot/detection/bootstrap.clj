(ns camelot.detection.bootstrap
  (:require
   [camelot.model.trap-station-session-camera :as session-camera]
   [camelot.model.media :as media]
   [camelot.detection.state :as state]
   [camelot.detection.event :as event]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]))

(def ^:private max-timeout-ms (* 900 1000))
(def ^:private base-timeout-ms (* 300 1000))

(defn- upload-complete?
  [media]
  (let [thresh (t/minus (t/now) (t/seconds 60))]
    (t/after? thresh (:media-created media))))

(defn- eligible-for-detection?
  [state session-camera]
  (boolean (some->> session-camera
                    :trap-station-session-camera-id
                    (media/get-most-recent-upload state)
                    upload-complete?)))

(defn- retrieve-tasks
  [state detector-state]
  (->> (session-camera/get-all* state)
       (remove #(= (state/session-camera-status detector-state (:trap-station-session-camera-id %))
                   :no-action))
       (remove #(state/all-processing-completed-for-task? detector-state (:trap-station-session-camera-id %)))
       (filter (partial eligible-for-detection? state))
       (map event/to-prepare-task-event)))

(defn run
  "Queue session cameras for preparation."
  [state detector-state-ref event-ch]
  (let [ch (async/chan 1)
        int-ch (async/chan (async/dropping-buffer 100000))]
    (log/info "Queuing session cameras")
    (async/go
      (async/>! event-ch {:action :bootstrap-retrieve
                          :subject :global})
      (log/warn "Found tasks:" (count (retrieve-tasks state @detector-state-ref)))
      (doseq [batch (retrieve-tasks state @detector-state-ref)]
        (async/>! int-ch batch)))
    (async/go-loop [timeout-ms base-timeout-ms]
      (let [timeout-ch (async/timeout timeout-ms)
            [v port] (async/alts! [int-ch timeout-ch] :priority true)]
        (condp = port
          int-ch
          (do
            (async/>! ch v)
            (log/info "Session camera queued" (:subject-id v))
            (async/>! event-ch {:action :bootstrap-schedule
                                :subject :session-camera
                                :subject-id (:subject-id v)})
            (recur timeout-ms))

          timeout-ch
          (do
            (log/info "Re-queuing session cameras")
            (async/go
              (doseq [batch (retrieve-tasks state @detector-state-ref)]
                (async/>! int-ch batch)))
            (async/>! event-ch {:action :bootstrap-timeout
                                :subject :global})
            (recur (min (* timeout-ms 1.2) max-timeout-ms))))))
    ch))
