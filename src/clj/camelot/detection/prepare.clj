(ns camelot.detection.prepare
  (:require
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [camelot.detection.util :as util]
   [camelot.model.media :as media]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(def ^:private media-per-task-limit 100000)

(defn- retrieve
  [state detector-state session-camera-id]
  (->> session-camera-id
       (media/get-all state)
       (take media-per-task-limit)
       (remove #(state/media-processing-completed? detector-state (:media-id %)))))

(defn run
  "Create tasks and queue media for upload."
  [state detector-state-ref ch cmd-mult [upload-ch upload-cmd-ch] [poll-ch _] event-ch]
  (let [cmd-ch (async/tap cmd-mult (async/chan))
        int-ch (async/chan (async/dropping-buffer 20000))]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch int-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (let [propagate-cmd (fn [v] (async/put! upload-cmd-ch v))]
            (condp = (:cmd v)
              :stop
              (do
                (log/info "Detector prepare stopped")
                (propagate-cmd v))

              :pause
              (do
                (propagate-cmd v)
                (util/pause cmd-ch #(propagate-cmd %)))

              (do
                (propagate-cmd v)
                (recur))))

          int-ch
          (do
            (async/>! upload-ch v)
            (recur))

          ch
          (do
            (let [scid (:subject-id v)]
              (log/info "Preparing task with scid " scid)
              (if (state/submitted-task-for-session-camera? @detector-state-ref scid)
                (let [task-id (:task (state/get-session-camera-state @detector-state-ref scid))]
                  (log/info "Task already submitted. Starting poll.")
                  (async/>! event-ch {:action :prepare-skip-task-submission
                                      :subject :task
                                      :subject-id task-id})
                  (async/>! poll-ch (event/to-check-result-event task-id)))
                (let [media (retrieve state @detector-state-ref scid)]
                  (if (seq media)
                    (do
                      (when-not (state/get-task-for-session-camera-id @detector-state-ref scid)
                        (log/info "Creating a new task...")
                        (try
                          (let [task (client/create-task state)]
                            (log/info "Created task " (:id task))
                            (state/add-task-to-state detector-state-ref scid task)
                            (async/>! event-ch {:action :prepare-task-created
                                                :subject :task
                                                :subject-id (:id task)}))
                          (catch Exception e
                            (async/>! event-ch {:action :prepare-task-create-failed
                                                :subject :trap-station-session-camera
                                                :subject-id scid})
                            (log/warn "Failed to create task:" e))))
                      (if-let [task-id (:task (state/get-session-camera-state @detector-state-ref scid))]
                        (do
                          (doseq [m media]
                            (let [media-id (:media-id m)]
                              (when-not (state/media-uploaded? @detector-state-ref media-id)
                                (state/record-media-upload! detector-state-ref scid media-id "pending")
                                (let [event (event/to-upload-media-event m scid)]
                                  (log/info "Queuing media for upload:" media-id)
                                  (async/>! int-ch event)))))
                          (log/info "Queuing presubmit check for task:" task-id)
                          (async/>! int-ch (event/to-presubmit-check-event task-id)))
                        (do
                          (async/>! event-ch {:action :prepare-task-not-found
                                              :subject :trap-station-session-camera
                                              :subject-id scid})
                          (log/warn "Could not find task for session-camera" scid))))
                    (do
                      (state/record-session-camera-status! detector-state-ref scid :no-action)
                      (async/>! event-ch {:action :prepare-skip-due-to-no-media
                                          :subject :trap-station-session-camera
                                          :subject-id scid})
                      (log/warn "No media needing upload. Skipping session camera " scid))))))
            (recur)))))))
