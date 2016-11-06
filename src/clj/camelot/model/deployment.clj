(ns camelot.model.deployment
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [camelot.util.data :as data]
            [camelot.model.trap-station :as trap-station]
            [camelot.util.trap-station :as util.ts]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.trap-station-session-camera :as trap-station-session-camera]
            [camelot.util.deployment :as dep-util]
            [camelot.model.survey-site :as survey-site]
            [camelot.model.camera-status :as camera-status]
            [camelot.model.camera :as camera]
            [camelot.util.trap-station :as util.ts]
            [clojure.set :as set])
  (:import [camelot.model.trap_station_session_camera TrapStationSessionCamera]))

(sql/defqueries "sql/deployments.sql" {:connection db/spec})

(s/defrecord TDeployment
    [survey-id :- s/Int
     site-id :- s/Int
     trap-station-name :- s/Str
     trap-station-longitude :- s/Num
     trap-station-latitude :- s/Num
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-notes :- (s/maybe s/Str)
     primary-camera-id :- s/Int
     secondary-camera-id :- (s/maybe s/Int)])

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
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     trap-station-session-end-date :- (s/maybe org.joda.time.DateTime)
     primary-camera-id :- s/Int
     primary-camera-name :- s/Str
     primary-camera-status-id :- s/Int
     secondary-camera-id :- (s/maybe s/Num)
     secondary-camera-name :- (s/maybe s/Str)
     secondary-camera-status-id :- (s/maybe s/Int)])

(s/defn tdeployment
  [{:keys [survey-id site-id trap-station-name trap-station-longitude trap-station-latitude
           trap-station-altitude
           trap-station-distance-above-ground trap-station-distance-to-river
           trap-station-distance-to-road trap-station-distance-to-settlement
           trap-station-session-start-date trap-station-session-end-date trap-station-notes
           primary-camera-id secondary-camera-id]}]
  (->TDeployment survey-id site-id trap-station-name trap-station-longitude
                 trap-station-latitude trap-station-altitude
                 trap-station-distance-above-ground trap-station-distance-to-river
                 trap-station-distance-to-road trap-station-distance-to-settlement
                 trap-station-session-start-date trap-station-notes primary-camera-id
                 secondary-camera-id))

(s/defn deployment
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id trap-station-name
           site-id survey-site-id site-name trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-distance-above-ground trap-station-distance-to-river
           trap-station-distance-to-road trap-station-distance-to-settlement
           trap-station-notes trap-station-session-start-date
           trap-station-session-end-date primary-camera-id
           primary-camera-name primary-camera-status-id secondary-camera-id
           secondary-camera-name secondary-camera-status-id]}]
  (->Deployment trap-station-session-id trap-station-session-created
                trap-station-session-updated trap-station-id
                trap-station-name site-id survey-site-id site-name
                trap-station-longitude trap-station-latitude
                trap-station-altitude
                trap-station-distance-above-ground trap-station-distance-to-river
                trap-station-distance-to-road trap-station-distance-to-settlement
                trap-station-notes trap-station-session-start-date trap-station-session-end-date
                primary-camera-id primary-camera-name primary-camera-status-id
                secondary-camera-id secondary-camera-name
                secondary-camera-status-id))

(s/defn get-all :- [Deployment]
  [state :- State
   id :- s/Int]
  (->> {:survey-id id}
       (db/with-db-keys state -get-all)
       dep-util/assoc-cameras
       (group-by :trap-station-id)
       vals
       (map #(sort-by :trap-station-session-id %))
       (map last)
       (mapv deployment)))

(s/defn get-specific :- (s/maybe Deployment)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-id id}
           (db/with-db-keys state -get-specific)
           dep-util/assoc-cameras
           first
           deployment))

(s/defn activate-camera!
  [state :- State
   camera-id :- s/Int]
  (let [active-id (:camera-status-active-id state)]
    (camera/set-camera-status! state camera-id active-id)))

(defn create-session-camera!
  [state data]
  (->> data
       trap-station-session-camera/ttrap-station-session-camera
       (trap-station-session-camera/create!* state)))

(s/defn create-new-session!
  [state :- State
   data]
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

(s/defn update!
  "Update trap station details for a deployment."
  [state :- State
   id :- s/Int
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

(s/defn create!
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
