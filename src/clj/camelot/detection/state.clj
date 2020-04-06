(ns camelot.detection.state)

;; TODO write a spec for the detector-state
#_{:tasks {120 {:status "pending"
               :media-ids [1201 ...]}}
   :media {1201 {:status "completed"}}
   :session-cameras {1 {:task 120}}}

(defn get-task
  "Return the task state with the given id."
  [detector-state task-id]
  (get-in detector-state [:tasks task-id]))

(defn get-session-camera-state
  "Return the session-camera state with the given id."
  [detector-state session-camera-id]
  (get-in detector-state [:session-cameras session-camera-id]))

(defn get-task-id-for-session-camera-id
  "Return the task-id for the given `session-camera-id`."
  [detector-state session-camera-id]
  (:task (get-session-camera-state detector-state session-camera-id)))

(defn get-task-for-session-camera-id
  "Return the task for the given `session-camera-id`."
  [detector-state session-camera-id]
  (let [task-id (:task (get-session-camera-state detector-state session-camera-id))]
    (get-task detector-state task-id)))

(defn get-media-state
  "Return the state for a given media"
  [detector-state media-id]
  (get-in detector-state [:media media-id]))

(defn media-uploaded?
  "Return `true` if the media has been uploaded"
  [detector-state media-id]
  (= (:status (get-media-state detector-state media-id)) "completed"))

(defn- narrow-task
  [task]
  (let [container (select-keys (:container task) [:readwrite_sas])]
    (-> task
        (select-keys [:status])
        (assoc :container container))))

(defn add-task-to-state
  "Add a new `task` into state for `session-camera-id`."
  [detector-state-ref session-camera-id task]
  (letfn [(assoc-task
            [detector-state]
            (-> detector-state
                (assoc-in [:session-cameras session-camera-id :task] (:id task))
                (assoc-in [:tasks (:id task)]
                          (-> task
                              narrow-task
                              (assoc :session-camera session-camera-id)))))]
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
    (assoc-in detector-state [:media media-id :status] status)
    (-> detector-state
        (assoc-in [:media media-id :status] "failed")
        (update-in [:media media-id :retries] (fnil inc 0)))))

(defn record-media-upload!
  "Update the upload status for `media-id`."
  [detector-state-ref scid media-id status]
  (let [task-id (:task (get-session-camera-state @detector-state-ref scid))]
    (swap! detector-state-ref
           #(-> %
                (update-upload-status media-id status)
                (update-in [:tasks task-id :media-ids] (fnil conj #{}) media-id)))))

(defn- upload-retry-limit-reached?
  [media]
  (>= (:retries media) upload-retry-limit))

(defn media-upload-failed?
  [detector-state media-id]
  (= (:status (get-media-state detector-state media-id)) "failed"))

(defn can-upload?
  "Predicate returning `true` if the media can be uploaded. False otherwise."
  [detector-state media-id]
  (let [media (get-media-state detector-state media-id)]
    (boolean
     (or (= (:status media) "pending")
         (and (= (:status media) "failed")
              (not (upload-retry-limit-reached? media)))))))

(defn upload-completed?
  [detector-state media-id]
  (let [media (get-in detector-state [:media media-id])]
    (= (:status media) "completed")))

(defn set-task-status!
  ([detector-state-ref task-id status]
   (set-task-status! detector-state-ref task-id status {}))
  ([detector-state-ref task-id status extra]
   (swap! detector-state-ref
          (fn [s]
            (-> s
                (update-in [:tasks task-id] #(merge % extra))
                (assoc-in [:tasks task-id :status] status))))))

(defn task-status-by-session-camera-id
  [detector-state scid]
  (:status (get-task-for-session-camera-id detector-state scid)))

(defn submitted-task?
  [detector-state task-id]
  (= (get-task detector-state task-id) "submitted"))

(defn completed-task?
  [detector-state task-id]
  (or (= (get-task detector-state task-id) "completed")
      (= (get-task detector-state task-id) "archived")))

(defn submitted-task-for-session-camera?
  [detector-state scid]
  (submitted-task? detector-state (get-task-id-for-session-camera-id detector-state scid)))

(defn media-processing-status
  [detection-state media-id]
  (get-in detection-state [:media media-id :processing-status]))

(defn- media-fully-processed?
  [media]
  (and (= (:status media) "completed")
       (= (:processing-status media) "completed")))

(defn media-processing-completed?
  [detection-state media-id]
  (boolean
   (if-let [media (get-media-state detection-state media-id)]
     (or (and (= (:status media) "failed")
              (upload-retry-limit-reached? media))
         (= (:status media) "skipped")
         (media-fully-processed? media)))))

(defn set-media-processing-status!
  [detector-state-ref media-id status]
  (swap! detector-state-ref assoc-in [:media media-id :processing-status] status))

(defn all-processing-completed-for-task?
  [detector-state scid]
  (let [task (get-task-for-session-camera-id detector-state scid)]
    (boolean (and task
                  (or (= (:status task) "failed")
                      (= (:status task) "archived")
                      (and (= (:status task) "completed")
                           (every? #(media-processing-completed? detector-state %)
                                   (:media-ids task))))))))

(defn record-session-camera-status!
  [detector-state-ref scid status]
  (swap! detector-state-ref assoc-in [:session-cameras scid :status] status))

(defn session-camera-status
  [detector-state scid]
  (get-in detector-state [:session-cameras scid :status]))
