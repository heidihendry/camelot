(ns camelot.model.deployment
  "Deployment model and data access."
  (:require
   [schema.core :as sch]
   [clj-time.core :as t]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.util.data :as data]
   [camelot.model.trap-station :as trap-station]
   [camelot.util.trap-station :as utilts]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.util.deployment :as dep-util]
   [camelot.model.survey-site :as survey-site]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.camera :as camera])
  (:import
   (camelot.model.trap_station_session_camera TrapStationSessionCamera)))

(def query (db/with-db-keys :deployments))

(sch/defrecord TDeployment
    [survey-id :- sch/Int
     site-id :- sch/Int
     trap-station-name :- sch/Str
     trap-station-longitude :- sch/Num
     trap-station-latitude :- sch/Num
     trap-station-altitude :- (sch/maybe sch/Num)
     trap-station-distance-above-ground :- (sch/maybe sch/Num)
     trap-station-distance-to-river :- (sch/maybe sch/Num)
     trap-station-distance-to-road :- (sch/maybe sch/Num)
     trap-station-distance-to-settlement :- (sch/maybe sch/Num)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-notes :- (sch/maybe sch/Str)
     primary-camera-id :- sch/Int
     secondary-camera-id :- (sch/maybe sch/Int)]
  {sch/Any sch/Any})

(sch/defrecord Deployment
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
     trap-station-session-end-date :- (sch/maybe org.joda.time.DateTime)
     primary-camera-id :- (sch/maybe sch/Int)
     primary-camera-name :- (sch/maybe sch/Str)
     primary-camera-status-id :- (sch/maybe sch/Int)
     secondary-camera-id :- (sch/maybe sch/Num)
     secondary-camera-name :- (sch/maybe sch/Str)
     secondary-camera-status-id :- (sch/maybe sch/Int)]
  {sch/Any sch/Any})

(def deployment map->Deployment)
(def tdeployment map->TDeployment)

(defn after?
  [a b]
  (cond
    (nil? a) true
    (nil? b) false
    :else (t/after? a b)))

(sch/defn get-all :- [Deployment]
  [state :- State
   id :- sch/Int]
  (->> {:survey-id id}
       (query state :get-all)
       dep-util/assoc-cameras
       (group-by :trap-station-id)
       vals
       (map #(sort-by :trap-station-session-end-date after? %))
       (map first)
       (mapv deployment)))

(sch/defn get-specific :- (sch/maybe Deployment)
  [state :- State
   id :- sch/Int]
  (some->> {:trap-station-session-id id}
           (query state :get-specific)
           dep-util/assoc-cameras
           first
           deployment))

(sch/defn activate-camera!
  [state :- State
   camera-id :- sch/Int]
  (let [active-id (:camera-status-active-id state)]
    (camera/set-camera-status! state camera-id active-id)))

(defn create-session-camera!
  [state data]
  (->> data
       trap-station-session-camera/ttrap-station-session-camera
       (trap-station-session-camera/create!* state)))

(sch/defn create-new-session!
  [state :- State
   data]
  (let [new-start (or (:trap-station-session-end-date data)
                      (:trap-station-session-start-date data))
        sdata {:trap-station-id (:trap-station-id data)
               :trap-station-session-start-date new-start}]
    (->> sdata
         trap-station-session/ttrap-station-session
         (trap-station-session/create! state))))

(sch/defn create-new-session-and-cameras!
  [state :- State
   data]
  (let [s (create-new-session! state data)]
    (doseq [c (:cameras data)]
      (create-session-camera! state (merge s c)))
    s))

(defn- get-or-create-survey-site!
  [s data]
  (->> data survey-site/tsurvey-site (survey-site/get-or-create! s)))

(defn- update-trap-station!
  [s id data]
  (->> data
       trap-station/ttrap-station
       (trap-station/update! s id)))

(sch/defn update!
  "Update trap station details for a deployment."
  [state :- State
   id :- sch/Int
   data :- TDeployment]
  (db/with-transaction [s state]
    (->> (get-or-create-survey-site! s data)
         (data/select-keys-inv [:survey-site-id])
         (merge data)
         (update-trap-station! s id))))

(defn- create-trap-station!
  [s data]
  (->> data
       trap-station/ttrap-station
       (trap-station/create! s)))

(sch/defn create!
  [state :- State
   data :- TDeployment]
  (db/with-transaction [s (assoc state :camera-status-active-id
                                 (camera-status/active-status-id state))]
    (let [d (dep-util/map-with-cameras-as-list data)
          ss (get-or-create-survey-site! s d)
          ts (create-trap-station!
              s (merge data (select-keys ss [:survey-site-id])))]
      (doseq [c (:cameras d)]
        (activate-camera! s (:camera-id c)))
      (create-new-session-and-cameras!
       s (merge d (select-keys ts [:trap-station-id]))))))
