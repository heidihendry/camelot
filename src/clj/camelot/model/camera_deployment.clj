(ns camelot.model.camera-deployment
  "Camera deployment / camera check model and data access."
  (:require
   [schema.core :as sch]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]
   [clj-time.core :as t]
   [camelot.util.trap-station :as utilts]
   [camelot.model.camera :as camera]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.data :as data]
   [camelot.util.deployment :as dep-util]
   [camelot.model.deployment :as deployment]))

(def query (db/with-db-keys :deployments))

(sch/defrecord TCameraDeployment
    [trap-station-session-id :- sch/Int
     trap-station-name :- sch/Str
     site-id :- sch/Int
     trap-station-id :- sch/Int
     trap-station-longitude
     trap-station-latitude
     trap-station-altitude :- (sch/maybe sch/Num)
     trap-station-distance-above-ground :- (sch/maybe sch/Num)
     trap-station-distance-to-river :- (sch/maybe sch/Num)
     trap-station-distance-to-road :- (sch/maybe sch/Num)
     trap-station-distance-to-settlement :- (sch/maybe sch/Num)
     trap-station-notes :- (sch/maybe sch/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     primary-camera-id :- sch/Int
     primary-camera-name :- sch/Str
     primary-camera-original-id :- sch/Int
     primary-camera-status-id :- sch/Int
     primary-camera-media-unrecoverable :- sch/Bool
     secondary-camera-id :- (sch/maybe sch/Int)
     secondary-camera-name :- (sch/maybe sch/Str)
     secondary-camera-original-id :- (sch/maybe sch/Int)
     secondary-camera-status-id :- (sch/maybe sch/Int)
     secondary-camera-media-unrecoverable :- (sch/maybe sch/Bool)]
  {sch/Any sch/Any})

(sch/defrecord CameraDeployment
    [trap-station-session-id :- sch/Int
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id :- sch/Int
     trap-station-name :- sch/Str
     site-id :- sch/Int
     survey-site-id :- sch/Int
     site-name :- sch/Str
     trap-station-longitude :- (sch/pred utilts/valid-longitude?)
     trap-station-latitude :- (sch/pred utilts/valid-latitude?)
     trap-station-altitude :- (sch/maybe sch/Num)
     trap-station-distance-above-ground :- (sch/maybe sch/Num)
     trap-station-distance-to-river :- (sch/maybe sch/Num)
     trap-station-distance-to-road :- (sch/maybe sch/Num)
     trap-station-distance-to-settlement :- (sch/maybe sch/Num)
     trap-station-notes :- (sch/maybe sch/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- org.joda.time.DateTime
     trap-station-session-camera-id :- sch/Int
     trap-station-session-camera-media-unrecoverable :- sch/Bool
     camera-id :- sch/Int
     camera-name :- sch/Str
     camera-status-id :- sch/Int
     has-uploaded-media :- sch/Bool]
  {sch/Any sch/Any})

(def camera-deployment map->CameraDeployment)
(def tcamera-deployment map->TCameraDeployment)

(sch/defn update-used-cameras!
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

(sch/defn maybe-create-new-session-and-cameras!
  [state :- State
   data]
  {:pre [(data/nat? (:trap-station-session-id data))]}
  (let [d (update data :cameras
                  #(filter (partial dep-util/camera-active?
                                    (:camera-status-active-id state)) %))]
    (when (seq (:cameras d))
      (deployment/create-new-session-and-cameras! state d))))

(sch/defn get-uploaded-status
  [state rec]
  (assoc rec :has-uploaded-media
         (or (->> rec
                  (data/select-keys-inv [:trap-station-session-camera-id])
                  (query state :get-uploaded-status)
                  first
                  :has-uploaded-media)
             false)))

(sch/defn get-uploadable :- [CameraDeployment]
  [state :- State
   id :- sch/Int]
  (->> {:survey-id id}
       (query state :get-uploadable)
       (map (partial get-uploaded-status state))
       (map camera-deployment)))

(defn valid-camera-check?
  "Predicate returning false if the camera check is invalid. True otherwise."
  [data]
  (and (not= (get data (dep-util/camera-id-key :primary))
             (get data (dep-util/camera-id-key :secondary)))
       (not (t/after? (:trap-station-session-start-date data)
                      (:trap-station-session-end-date data)))))

(sch/defn create-camera-check!
  [state :- State
   data :- TCameraDeployment]
  {:pre [(valid-camera-check? data)]}
  (let [d (dep-util/map-with-cameras-as-list data)]
    (db/with-transaction [s (assoc state :camera-status-active-id
                                   (camera-status/active-status-id state))]
      (trap-station-session/set-session-end-date! s d)
      (update-used-cameras! s d)
      (maybe-create-new-session-and-cameras! s d))))
