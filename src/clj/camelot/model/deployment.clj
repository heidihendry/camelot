(ns camelot.model.deployment
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [camelot.model.trap-station :as trap-station]
            [camelot.util.trap-station :as util.ts]
            [clj-time.core :as t]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.trap-station-session-camera :as trap-station-session-camera]
            [camelot.model.survey-site :as survey-site]
            [camelot.model.camera-status :as camera-status]
            [camelot.util.trap-station :as util.ts]))

(sql/defqueries "sql/deployments.sql" {:connection db/spec})

(s/defrecord TDeployment
    [survey-id :- s/Int
     site-id :- s/Int
     trap-station-name :- s/Str
     trap-station-longitude :- s/Num
     trap-station-latitude :- s/Num
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-session-start-date :- org.joda.time.DateTime
     primary-camera-id :- s/Int
     secondary-camera-id :- (s/maybe s/Int)])

(s/defrecord TCameraDeployment
    [trap-station-session-id :- s/Int
     trap-station-name :- s/Str
     site-id :- s/Int
     trap-station-id :- s/Int
     trap-station-longitude
     trap-station-latitude
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     primary-camera-id :- s/Int
     primary-camera-name :- s/Str
     primary-camera-status-id :- s/Int
     secondary-camera-id :- (s/maybe s/Int)
     secondary-camera-name :- (s/maybe s/Str)
     secondary-camera-status-id :- (s/maybe s/Int)])

(s/defrecord Deployment
    [trap-station-session-id :- s/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- s/Int
     trap-station-name :- s/Str
     site-id :- s/Int
     survey-site-id :- s/Int
     site-name :- s/Str
     trap-station-longitude :- (s/pred util.ts/valid-longitude?)
     trap-station-latitude :- (s/pred util.ts/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     primary-camera-id :- s/Int
     primary-camera-name :- s/Str
     primary-camera-status-id :- s/Int
     secondary-camera-id :- (s/maybe s/Num)
     secondary-camera-name :- (s/maybe s/Str)
     secondary-camera-status-id :- (s/maybe s/Int)])

(s/defrecord CameraDeployment
    [trap-station-session-id :- s/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- s/Int
     trap-station-name :- s/Str
     site-id :- s/Int
     survey-site-id :- s/Int
     site-name :- s/Str
     trap-station-longitude :- (s/pred util.ts/valid-longitude?)
     trap-station-latitude :- (s/pred util.ts/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     trap-station-session-camera-id :- s/Int
     camera-id :- s/Int
     camera-name :- s/Str
     camera-status-id :- s/Int])

(s/defn camera-id-key
  [cam-type]
  (keyword (str (name cam-type) "-id")))

(s/defn camera-status-id-key
  [cam-type]
  (keyword (str (name cam-type) "-status-id")))

(defn validate-camera-check
  [state data]
  (and (not= (get data (camera-id-key :primary-camera))
             (get data (camera-id-key :secondary-camera)))
       (not (t/after? (:trap-station-session-start-date data)
                      (:trap-station-session-end-date data)))))

(s/defn tdeployment
  [{:keys [survey-id site-id trap-station-name trap-station-longitude trap-station-latitude
           trap-station-altitude trap-station-session-start-date
           primary-camera-id secondary-camera-id]}]
  (->TDeployment survey-id site-id trap-station-name trap-station-longitude
                 trap-station-latitude trap-station-altitude
                 trap-station-session-start-date primary-camera-id secondary-camera-id))

(s/defn deployment
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id trap-station-name
           site-id survey-site-id site-name trap-station-longitude
           trap-station-latitude trap-station-altitude trap-station-notes
           trap-station-session-start-date primary-camera-id
           primary-camera-name primary-camera-status-id secondary-camera-id
           secondary-camera-name secondary-camera-status-id]}]
  (->Deployment trap-station-session-id trap-station-session-created
                trap-station-session-updated trap-station-id
                trap-station-name site-id survey-site-id site-name
                trap-station-longitude trap-station-latitude
                trap-station-altitude trap-station-notes
                trap-station-session-start-date primary-camera-id
                primary-camera-name primary-camera-status-id
                secondary-camera-id secondary-camera-name
                secondary-camera-status-id))

(s/defn camera-deployment
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id trap-station-name
           site-id survey-site-id site-name trap-station-longitude
           trap-station-latitude trap-station-altitude trap-station-notes
           trap-station-session-start-date trap-station-session-end-date
           trap-station-session-camera-id
           camera-id camera-name camera-status-id]}]
  (->CameraDeployment trap-station-session-id trap-station-session-created
                      trap-station-session-updated trap-station-id
                      trap-station-name site-id survey-site-id site-name
                      trap-station-longitude trap-station-latitude
                      trap-station-altitude trap-station-notes
                      trap-station-session-start-date trap-station-session-end-date
                      trap-station-session-camera-id
                      camera-id camera-name camera-status-id))

(s/defn tcamera-deployment
  [{:keys [trap-station-session-id trap-station-name site-id trap-station-id
           trap-station-longitude trap-station-latitude trap-station-altitude
           trap-station-notes trap-station-session-start-date
           trap-station-session-end-date primary-camera-id primary-camera-name
           primary-camera-status-id secondary-camera-id secondary-camera-name
           secondary-camera-status-id]}]
  (->TCameraDeployment trap-station-session-id trap-station-name site-id trap-station-id
                       trap-station-longitude trap-station-latitude
                       trap-station-altitude trap-station-notes
                       trap-station-session-start-date trap-station-session-end-date
                       primary-camera-id primary-camera-name primary-camera-status-id
                       secondary-camera-id secondary-camera-name
                       secondary-camera-status-id))

(defn assoc-cameras-for-group
  [[session-id group]]
  (let [g1 (first group)
        g2 (second group)]
    (assoc
     (if g2
       (assoc g1
              :secondary-camera-id (:camera-id g2)
              :secondary-camera-name (:camera-name g2)
              :secondary-camera-status-id (:camera-status-id g2))
       g1)
     :primary-camera-id (:camera-id g1)
     :primary-camera-name (:camera-name g1)
     :primary-camera-status-id (:camera-status-id g1))))

(defn assoc-cameras
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (map assoc-cameras-for-group)))

(s/defn get-all :- [Deployment]
  [state :- State
   id :- s/Int]
  (->> {:survey-id id}
       (db/with-db-keys state -get-all)
       assoc-cameras
       (map deployment)))

(s/defn get-awaiting-upload :- [CameraDeployment]
  [state :- State
   id :- s/Int]
  (->> {:survey-id id}
       (db/with-db-keys state -get-awaiting-upload)
       (map camera-deployment)))

(s/defn get-specific :- (s/maybe Deployment)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-id id}
           (db/with-db-keys state -get-specific)
           assoc-cameras
           first
           deployment))

(s/defn set-session-end-date!
  [state :- State
   data]
  (db/with-db-keys state -set-session-end-date! data))

(s/defn set-camera-status!
  [state :- State
   cam-id
   cam-status]
  (db/with-db-keys state -set-camera-status!
    {:camera-status-id cam-status
     :camera-id cam-id}))

(s/defn active-status-id
  [state]
  (->> "camera-status/active"
       (camera-status/get-specific-with-description state)
       :camera-status-id))

(s/defn camera-changed?
  [orig-data
   data
   cam-type]
  (not= (get data (camera-id-key cam-type)) (get orig-data (camera-id-key cam-type))))

(s/defn activate-cameras!
  [state :- State
   data]
  (let [active-id (active-status-id state)
        orig-data (and (:trap-station-session-id data)
                       (get-specific state (:trap-station-session-id data)))]
    (if (camera-changed? orig-data data :primary-camera)
      (set-camera-status! state (get data (camera-id-key :primary-camera)) active-id)
      (set-camera-status! state (get data (camera-id-key :primary-camera))
                          (get data (camera-status-id-key :primary-camera))))
    (if (camera-changed? orig-data data :secondary-camera)
      (set-camera-status! state (get data (camera-id-key :secondary-camera)) active-id)
      (set-camera-status! state (get data (camera-id-key :secondary-camera))
                          (get data (camera-status-id-key :secondary-camera))))))

(s/defn update-used-camera!
  [state :- State
   orig-data
   data
   cam-type]
  (when (and (camera-status-changed? orig-data data cam-type)
             (get orig-data (camera-id-key cam-type)))
    (set-camera-status! state
                        (get orig-data (camera-id-key cam-type))
                        (get data (camera-status-id-key cam-type)))))

(s/defn update-used-cameras!
  [state :- State
   data]
  (let [orig-data (and (:trap-station-session-id data)
                       (get-specific state (:trap-station-session-id data)))
        active-id (active-status-id state)]
    (update-used-camera! state orig-data data :primary-camera)
    (update-used-camera! state orig-data data :secondary-camera)))

(s/defn update-cameras!
  [state
   data]
  (when (:trap-station-session-id data)
    (update-used-cameras! state data))
  (activate-cameras! state data))

(s/defn camera-active-or-added-fn
  [state orig-data data]
  (let [active-id (active-status-id state)]
    (fn [cam-type]
      (let [idk (camera-id-key cam-type)
            statk (camera-status-id-key cam-type)]
        (and (get data idk)
             (or (= (get data statk) active-id)
                 (not= (get data idk) (get orig-data idk))))))))

(s/defn create-session-camera!
  [state data session cam-type]
  (trap-station-session-camera/create!
   state
   (trap-station-session-camera/ttrap-station-session-camera
    {:trap-station-session-id (:trap-station-session-id session)
     :camera-id (get data (camera-id-key cam-type))})))

(s/defn create-new-session!
  [state data]
  (let [new-start (or (:trap-station-session-end-date data)
                      (:trap-station-session-start-date data))
        sdata {:trap-station-id (:trap-station-id data)
               :trap-station-session-start-date new-start}]
    (->> sdata
         trap-station-session/ttrap-station-session
         (trap-station-session/create! state))))

(s/defn create-new-session-and-cameras!
  [state :- State
   data]
  (let [orig-data (when (:trap-station-session-id data)
                    (get-specific state (:trap-station-session-id data)))
        camera-active? (camera-active-or-added-fn state orig-data data)]
    (when (or (nil? orig-data)
              (camera-active? :primary-camera)
              (camera-active? :secondary-camera))
      (let [s (create-new-session! state data)]
        (when (camera-active? :primary-camera)
          (create-session-camera! state data s :primary-camera))
        (when (camera-active? :secondary-camera)
          (create-session-camera! state data s :secondary-camera))
        s))))

(s/defn create-camera-check!
  [state :- State
   data :- TCameraDeployment]
  (if (validate-camera-check state data)
    (db/with-transaction [s state]
      (set-session-end-date! s data)
      (update-cameras! s data)
      (create-new-session-and-cameras! s data))
    (throw (RuntimeException. "Invalid camera check"))))

(s/defn create!
  [state :- State
   data :- TDeployment]
  (db/with-transaction [s state]
    (let [tss (survey-site/tsurvey-site
               (select-keys data [:survey-id :site-id]))
          ss (survey-site/get-or-create! s tss)
          ts (trap-station/create! s
                                   (trap-station/ttrap-station
                                    (merge data
                                           (select-keys ss [:survey-site-id]))))]
      (activate-cameras! s data)
      (create-new-session-and-cameras! s (merge data
                                                (select-keys ts [:trap-station-id]))))))
