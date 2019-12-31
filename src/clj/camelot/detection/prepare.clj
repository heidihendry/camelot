(ns camelot.detection.prepare
  (:require
   [camelot.services.analytics :as analytics]
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [camelot.model.media :as media]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defn- unprocessed-media?
  [detector-state media]
  (letfn [(unprocessed? [media-state]
            (let [status (:upload-status media-state)]
              (and (not= (:media-processed media) true)
                   (or (nil? status)
                       (= status "pending")
                       (and (= status "failed")
                            (< (:retries media-state) 3))))))]
    (->> (:media-id media)
         (state/get-media-state detector-state)
         unprocessed?)))

(defn- retrieve
  [state detector-state session-camera-id]
  (->> session-camera-id
       (media/get-all state)
       (filter (partial unprocessed-media? detector-state))))

(defn run
  "Create tasks and queue media for upload."
  [state detector-state-ref cmd-mult upload-ch poll-ch event-ch]
  (let [cmd-ch (async/chan)
        ch (async/chan 1)]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector prepare stopped")
            (recur))

          ch
          (do
            (async/>! event-ch v)
            (let [scid (:subject-id v)]
              (log/info "Preparing task with scid " scid)
              (if (state/submitted-task? @detector-state-ref scid)
                (let [task-id (:task (state/get-session-camera-state @detector-state-ref scid))]
                  (log/info "Task already submitted. Starting poll.")
                  (analytics/track state {:category "detector"
                                          :action "prepare-skip-task-submission"
                                          :label "task"
                                          :label-value task-id
                                          :ni true})
                  (async/>! poll-ch (event/to-check-result-event task-id)))
                (let [media (retrieve state @detector-state-ref scid)]
                  (if (seq media)
                    (do
                      (when-not (state/get-task-for-session-camera-id @detector-state-ref scid)
                        (log/info "Creating a new task...")
                        (if-let [task (client/create-task state)]
                          (do
                            (log/info "Created task " (:id task))
                            (state/add-task-to-state detector-state-ref scid task)
                            (analytics/track state {:category "detector"
                                                    :action "prepare-task-created"
                                                    :label "task"
                                                    :label-value (:id task)
                                                    :ni true}))
                          (log/warn "Failed to create task")))
                      (if-let [task-id (:task (state/get-session-camera-state @detector-state-ref scid))]
                        (do
                          (doseq [m media]
                            (state/record-media-upload! detector-state-ref scid (:media-id m) "pending")
                            (let [event (event/to-upload-media-event m scid)]
                              (log/info "Queuing media for upload: " (:subject-id event))
                              (async/>! upload-ch event)))
                          (log/info "Queuing presubmit check for task: " task-id)
                          (async/>! upload-ch (event/to-presubmit-check-event task-id)))
                        (log/warn "Could not find task for session-camera" scid)))
                    (log/warn "No media needing upload. Skipping session camera " scid)))))
            (recur)))))
    ch))
