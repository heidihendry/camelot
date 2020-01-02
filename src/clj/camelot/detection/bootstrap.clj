(ns camelot.detection.bootstrap
  (:require
   [camelot.model.trap-station-session-camera :as session-camera]
   [camelot.model.media :as media]
   [camelot.detection.state :as state]
   [camelot.detection.event :as event]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]))

(def ^:private reschedule-all (* 60 1000))

(defn- upload-complete?
  [media]
  (let [thresh (t/minus (t/now) (t/minutes 2))]
    (t/before? (:media-created media) thresh)))

(defn- eligible-for-detection?
  [state session-camera]
  (boolean (some->> session-camera
                    :trap-station-session-camera-id
                    (media/get-most-recent-upload state)
                    upload-complete?)))

(defn- retrieve-tasks
  [state detector-state]
  (->> (session-camera/get-all* state)
       (filter (partial eligible-for-detection? state))
       (remove #(= (state/session-camera-status detector-state (:trap-station-session-camera-id %))
                   :no-action))
       (remove #(state/completed-task? detector-state (:trap-station-session-camera-id %)))
       (map event/to-prepare-task-event)))

(defn run
  "Queue session cameras for preparation."
  [state detector-state-ref cmd-mult prepare-ch event-ch]
  (let [cmd-ch (async/tap cmd-mult (async/chan))
        int-ch (async/chan (async/dropping-buffer 100000))]
    (async/go-loop []
      (log/info "Queuing session cameras")
      (async/>! event-ch {:action :bootstrap-retrieve
                          :subject :global})
      (doseq [batch (retrieve-tasks state @detector-state-ref)]
        (async/>! int-ch batch))

      (let [timeout-ch (async/timeout reschedule-all)
            [v port] (async/alts! [cmd-ch int-ch timeout-ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (log/info "Detector bootstrap stopped")

            :pause
            (do
              (loop []
                (let [{:keys [cmd]} (async/<! cmd-ch)]
                  (when-not (= cmd :resume)
                    (recur))))
              (recur))

            (recur))

          int-ch
          (do
            (async/>! prepare-ch v)
            (log/info "Session camera queued" (:subject-id v))
            (async/>! event-ch {:action :bootstrap-schedule
                                :subject :session-camera
                                :subject-id (:subject-id v)})
            (recur))

          timeout-ch
          (do
            (log/info "Re-queuing session cameras")
            (recur)))))))
