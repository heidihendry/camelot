(ns camelot.detection.state)

;; TODO write a spec for the detector-state
#_{:tasks {120 {:status "pending"
               :media-ids [1201 ...]}}
   :media {1201 {:upload-status "completed"
                 :session-camera-id 1}}
   :session-cameras {1 {:task 120}}}

(defn get-task
  "Return the task state with the given id."
  [detector-state task-id]
  (get-in detector-state [:tasks task-id]))

(defn get-session-camera-state
  "Return the session-camera state with the given id."
  [detector-state session-camera-id]
  (get-in detector-state [:session-cameras session-camera-id]))

(defn get-task-for-session-camera-id
  "Return the task for the given `session-camera-id`."
  [detector-state session-camera-id]
  (let [task-id (:task (get-session-camera-state detector-state session-camera-id))]
    (get-task detector-state task-id)))

(defn get-media-state
  "Return the state for a given media"
  [detector-state media-id]
  (get-in detector-state [:media media-id]))

(defn add-task-to-state
  "Add a new `task` into state for `session-camera-id`."
  [detector-state-ref session-camera-id task]
  (letfn [(assoc-task
            [detector-state]
            (-> detector-state
                (assoc-in [:session-cameras session-camera-id :task] (:id task))
                (assoc-in [:tasks (:id task)]
                          (assoc task :session-camera session-camera-id))))]
    (swap! detector-state-ref assoc-task)))

(defn archive-task!
  [detector-state-ref task-id]
  (letfn [(archive! [detector-state]
            (-> detector-state
                (select-keys [:session-camera :media-ids])
                (assoc :status "archived")))]
    (swap! detector-state-ref update-in [:tasks task-id] archive!)))

(def ^:private upload-retry-limit 3)

(defn media-for-task
  [detector-state task-id]
  (get-in detector-state [:tasks task-id :media-ids]))

(defn- update-upload-status
  [detector-state media-id status]
  (if (not= status "failed")
    (assoc-in detector-state [:media media-id :upload-status] status)
    (-> detector-state
        (assoc-in [:media media-id :upload-status] "failed")
        (update-in [:media media-id :retries] (fnil inc 0)))))

(defn record-media-upload!
  "Update the upload status for `media-id`."
  [detector-state-ref scid media-id status]
  (let [task-id (:task (get-session-camera-state @detector-state-ref scid))]
    (swap! detector-state-ref
           #(-> %
                (update-upload-status media-id status)
                (update-in [:tasks task-id :media-ids] conj media-id)
                (assoc-in [:media media-id :session-camera-id] scid)))))

(defn upload-retry-limit-reached?
  "Predicate returning `true` if the retry limit has been reached for the given `media-id`."
  [detector-state media-id]
  (let [media (get-media-state detector-state media-id)]
    (and (= (:upload-status media) "failed")
         (>= (:retries media) upload-retry-limit))))

(defn upload-pending?
  [detector-state media-id]
  (let [media (get-in detector-state [:media media-id])]
    (or (= (:upload-status media) "pending")
        (and (= (:upload-status media) "failed")
             (not (upload-retry-limit-reached? detector-state media-id))))))

(defn upload-completed?
  [detector-state media-id]
  (let [media (get-in detector-state [:media media-id])]
    (= (:upload-status media) "completed")))

(defn set-task-status!
  [detector-state-ref task-id status]
  (swap! detector-state-ref assoc-in [:tasks task-id :status] status))

(defn task-status-by-session-camera-id
  [detector-state scid]
  (:status (get-task-for-session-camera-id detector-state scid)))

(defn unprocessed-task?
  [detector-state scid]
  (= (task-status-by-session-camera-id detector-state scid) "pending"))

(defn submitted-task?
  [detector-state scid]
  (= (task-status-by-session-camera-id detector-state scid) "submitted"))

(defn media-processing-status
  [detection-state media-id]
  (get-in detection-state [:media media-id :processing-status]))

(defn set-media-processing-status!
  [detector-state-ref media-id status]
  (swap! detector-state-ref assoc-in [:media media-id :processing-status] status))

(defn completed-task?
  [detector-state scid]
  (let [task (get-task-for-session-camera-id detector-state scid)]
    (boolean (and (not (nil? task))
                  (or (= (:status task) "failed")
                      (every? #(= (media-processing-status detector-state %) "completed")
                              (:media-ids task)))))))

(defn record-session-camera-status!
  [detector-state-ref scid status]
  (swap! detector-state-ref assoc-in [:session-cameras scid :status] status))

(defn session-camera-status
  [detector-state scid]
  (get-in detector-state [:session-cameras scid :status]))
