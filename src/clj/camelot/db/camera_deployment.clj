(ns camelot.db.camera-deployment
  "Camera deployment / camera check model and data access."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]
   [camelot.app.state :refer [State]]
   [clj-time.core :as t]
   [camelot.util.trap-station :as util.ts]
   [camelot.db.camera :as camera]
   [camelot.db.camera-status :as camera-status]
   [camelot.db.trap-station-session :as trap-station-session]
   [camelot.db.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.data :as data]
   [camelot.util.deployment :as dep-util]
   [camelot.db.deployment :as deployment]))

(sql/defqueries "sql/deployments.sql")

(s/defrecord TCameraDeployment
    [trap-station-session-id :- s/Int
     trap-station-name :- s/Str
     site-id :- s/Int
     trap-station-id :- s/Int
     trap-station-longitude
     trap-station-latitude
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     primary-camera-id :- s/Int
     primary-camera-name :- s/Str
     primary-camera-original-id :- s/Int
     primary-camera-status-id :- s/Int
     primary-camera-media-unrecoverable :- s/Bool
     secondary-camera-id :- (s/maybe s/Int)
     secondary-camera-name :- (s/maybe s/Str)
     secondary-camera-original-id :- (s/maybe s/Int)
     secondary-camera-status-id :- (s/maybe s/Int)
     secondary-camera-media-unrecoverable :- (s/maybe s/Bool)])

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
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     trap-station-session-camera-id :- s/Int
     trap-station-session-camera-media-unrecoverable :- s/Bool
     camera-id :- s/Int
     camera-name :- s/Str
     camera-status-id :- s/Int
     has-uploaded-media :- s/Bool])

(s/defn camera-deployment
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id trap-station-name
           site-id survey-site-id site-name trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-distance-above-ground trap-station-distance-to-river
           trap-station-distance-to-road trap-station-distance-to-settlement
           trap-station-notes
           trap-station-session-start-date trap-station-session-end-date
           trap-station-session-camera-id
           trap-station-session-camera-media-unrecoverable
           camera-id camera-name camera-status-id has-uploaded-media]}]
  (->CameraDeployment trap-station-session-id trap-station-session-created
                      trap-station-session-updated trap-station-id
                      trap-station-name site-id survey-site-id site-name
                      trap-station-longitude trap-station-latitude
                      trap-station-altitude
                      trap-station-distance-above-ground trap-station-distance-to-river
                      trap-station-distance-to-road trap-station-distance-to-settlement
                      trap-station-notes
                      trap-station-session-start-date trap-station-session-end-date
                      trap-station-session-camera-id
                      trap-station-session-camera-media-unrecoverable
                      camera-id camera-name camera-status-id has-uploaded-media))

(s/defn tcamera-deployment
  [{:keys [trap-station-session-id trap-station-name site-id trap-station-id
           trap-station-longitude trap-station-latitude trap-station-altitude
           trap-station-distance-above-ground trap-station-distance-to-river
           trap-station-distance-to-road trap-station-distance-to-settlement
           trap-station-notes trap-station-session-start-date
           trap-station-session-end-date primary-camera-id primary-camera-name
           primary-camera-original-id
           primary-camera-status-id primary-camera-media-unrecoverable
           secondary-camera-id secondary-camera-name
           secondary-camera-original-id
           secondary-camera-status-id secondary-camera-media-unrecoverable]}]
  (->TCameraDeployment trap-station-session-id trap-station-name site-id trap-station-id
                       trap-station-longitude trap-station-latitude
                       trap-station-altitude
                       trap-station-distance-above-ground trap-station-distance-to-river
                       trap-station-distance-to-road trap-station-distance-to-settlement
                       trap-station-notes
                       trap-station-session-start-date trap-station-session-end-date
                       primary-camera-id primary-camera-name
                       primary-camera-original-id primary-camera-status-id
                       primary-camera-media-unrecoverable
                       secondary-camera-id secondary-camera-name
                       secondary-camera-original-id
                       secondary-camera-status-id
                       secondary-camera-media-unrecoverable))

(s/defn update-used-cameras!
  "Update the status of cameras and the media recoverability flag."
  [state :- State
   data]
  (doseq [c (:cameras data)]
    (when (dep-util/original-camera-removed? (:camera-status-active-id state) c)
      (camera/set-camera-status! state (:camera-original-id c) (:camera-status-id c)))
    (when (not= (:camera-original-id c) (:camera-id c))
      (deployment/activate-camera! state (:camera-id c)))
    (when (data/nat? (:camera-original-id c))
      (trap-station-session-camera/update-media-unrecoverable!
       state (:camera-original-id c) (:trap-station-session-id data)
       (:camera-media-unrecoverable c)))))

(s/defn maybe-create-new-session-and-cameras!
  [state :- State
   data]
  {:pre [(data/nat? (:trap-station-session-id data))]}
  (let [d (update data :cameras
                  #(filter (partial dep-util/camera-active?
                                    (:camera-status-active-id state)) %))]
    (when (seq (:cameras d))
      (deployment/create-new-session-and-cameras! state d))))

(s/defn get-uploaded-status
  [state rec]
  (assoc rec :has-uploaded-media
         (or (->> rec
                  (data/select-keys-inv [:trap-station-session-camera-id])
                  (db/with-db-keys state -get-uploaded-status)
                  first
                  :has-uploaded-media)
             false)))

(s/defn get-uploadable :- [CameraDeployment]
  [state :- State
   id :- s/Int]
  (->> {:survey-id id}
       (db/with-db-keys state -get-uploadable)
       (map (partial get-uploaded-status state))
       (map camera-deployment)))

(defn- valid-camera-check?
  [data]
  (and (not= (get data (dep-util/camera-id-key :primary))
             (get data (dep-util/camera-id-key :secondary)))
       (not (t/after? (:trap-station-session-start-date data)
                      (:trap-station-session-end-date data)))))

(s/defn create-camera-check!
  [state :- State
   data :- TCameraDeployment]
  {:pre [(valid-camera-check? data)]}
  (let [d (dep-util/map-with-cameras-as-list data)]
    (db/with-transaction [s (assoc state :camera-status-active-id
                                   (camera-status/active-status-id state))]
      (trap-station-session/set-session-end-date! s d)
      (update-used-cameras! s d)
      (maybe-create-new-session-and-cameras! s d))))

