(ns camelot.model.deployment
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [camelot.model.trap-station :as trap-station]))

(sql/defqueries "sql/deployments.sql" {:connection db/spec})

(s/defrecord TDeployment
    [trap-station-name :- s/Str
     site-id :- s/Num
     trap-station-longitude :- (s/pred trap-station/valid-longitude?)
     trap-station-latitude :- (s/pred trap-station/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- (s/maybe org.joda.time.DateTime)
     primary-camera-id :- s/Num
     primary-camera-name :- s/Str
     secondary-camera-id :- (s/maybe s/Num)
     secondary-camera-name :- (s/maybe s/Str)])

(s/defrecord Deployment
    [trap-station-session-id :- s/Num
     trap-station-session-created :- org.joda.time.DateTime
     trap-station-session-updated :- org.joda.time.DateTime
     trap-station-id
     trap-station-name :- s/Str
     survey-site-id :- s/Num
     site-name :- s/Str
     trap-station-longitude :- (s/pred trap-station/valid-longitude?)
     trap-station-latitude :- (s/pred trap-station/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     primary-camera-id :- s/Num
     primary-camera-name :- s/Str
     secondary-camera-id :- (s/maybe s/Num)
     secondary-camera-name :- (s/maybe s/Str)])

(s/defn deployment
  [{:keys [trap-station-session-id trap-station-session-created trap-station-session-updated
           trap-station-id trap-station-name survey-site-id site-name
           trap-station-longitude trap-station-latitude trap-station-altitude
           trap-station-notes trap-station-session-start-date
           primary-camera-id
           primary-camera-name
           secondary-camera-id
           secondary-camera-name]}]
  (->Deployment trap-station-session-id trap-station-session-created
                trap-station-session-updated trap-station-session-id
                trap-station-name survey-site-id site-name
                trap-station-longitude trap-station-latitude
                trap-station-altitude trap-station-notes
                trap-station-session-start-date
                primary-camera-id
                primary-camera-name
                secondary-camera-id
                secondary-camera-name))

(s/defn tdeployment
  [{:keys [trap-station-name site-id trap-station-longitude
           trap-station-latitude trap-station-altitude trap-station-notes
           trap-station-session-start-date
           primary-camera-id
           primary-camera-name
           secondary-camera-id
           secondary-camera-name]}]
  (->TDeployment trap-station-name site-id trap-station-longitude
                 trap-station-latitude trap-station-altitude trap-station-notes
                 trap-station-session-start-date
                 primary-camera-id
                 primary-camera-name
                 secondary-camera-id
                 secondary-camera-name))

(defn assoc-cameras-for-group
  [[session-id group]]
  (let [g1 (first group)
        g2 (second group)]
    (assoc
     (if g2
       (assoc g1
              :secondary-camera-id (:camera-id g2)
              :secondary-camera-name (:camera-name g2))
       g1)
     :primary-camera-id (:camera-id g1)
     :primary-camera-name (:camera-name g1))))

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

(s/defn get-specific :- (s/maybe Deployment)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-session-id id}
           (db/with-db-keys state -get-specific)
           assoc-cameras
           first
           deployment))
