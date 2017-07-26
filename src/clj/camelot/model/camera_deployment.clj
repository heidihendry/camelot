(ns camelot.model.camera-deployment
  "Camera deployment / camera check model and data access."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.system.state :refer [State]]
   [clj-time.core :as t]
   [camelot.util.trap-station :as util.ts]
   [camelot.model.camera :as camera]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.data :as data]
   [camelot.util.deployment :as dep-util]
   [camelot.model.deployment :as deployment]))

(def query (db/with-db-keys :deployments))

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
     secondary-camera-media-unrecoverable :- (s/maybe s/Bool)]
  {s/Any s/Any})

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
     has-uploaded-media :- s/Bool]
  {s/Any s/Any})

(def camera-deployment map->CameraDeployment)
(def tcamera-deployment map->TCameraDeployment)

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
                  (query state :get-uploaded-status)
                  first
                  :has-uploaded-media)
             false)))

(s/defn get-uploadable :- [CameraDeployment]
  [state :- State
   id :- s/Int]
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
