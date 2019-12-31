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
       (remove #(state/completed-task? detector-state (:trap-station-session-camera-id %)))
       (map event/to-prepare-task-event)))

(defn run
  "Queue session cameras for preparation."
  [state detector-state-ref cmd-mult prepare-ch]
  (let [cmd-ch (async/chan)]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (log/info "Queuing session cameras")
      (doseq [batch (retrieve-tasks state @detector-state-ref)]
        (log/info "Session camera queued " (:subject-id batch))
        (async/>! prepare-ch batch))

      (let [timeout-ch (async/timeout reschedule-all)
            [v port] (async/alts! [cmd-ch timeout-ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector bootstrap stopped")
            (recur))

          timeout-ch
          (do
            (log/info "Re-queuing session cameras")
            (recur)))))))
