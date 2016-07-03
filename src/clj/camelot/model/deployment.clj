(ns camelot.model.deployment
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [camelot.model.trap-station :as trap-station]
            [clj-time.core :as t]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.trap-station-session-camera :as trap-station-session-camera]))

(sql/defqueries "sql/deployments.sql" {:connection db/spec})

(s/defrecord TDeployment
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
     trap-station-longitude :- (s/pred trap-station/valid-longitude?)
     trap-station-latitude :- (s/pred trap-station/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-session-start-date :- org.joda.time.DateTime
     primary-camera-id :- s/Int
     primary-camera-name :- s/Str
     primary-camera-status-id :- s/Int
     secondary-camera-id :- (s/maybe s/Num)
     secondary-camera-name :- (s/maybe s/Str)
     secondary-camera-status-id :- (s/maybe s/Int)])

(defn validate-camera-check
  [state data]
  (and (not= (:primary-camera-id data) (:secondary-camera-id data))
       (not (t/after? (:trap-station-session-start-date data)
                      (:trap-station-session-end-date data)))
       ;; TODO check camera statuses
       ))

(s/defn deployment
  [{:keys [trap-station-session-id trap-station-session-created
           trap-station-session-updated trap-station-id trap-station-name
           site-id survey-site-id site-name trap-station-longitude
           trap-station-latitude trap-station-altitude trap-station-notes
           trap-station-session-start-date primary-camera-id
           primary-camera-name primary-camera-status-id secondary-camera-id
           secondary-camera-name secondary-camera-status-id]}]
  (->Deployment trap-station-session-id trap-station-session-created
                trap-station-session-updated trap-station-session-id
                trap-station-name site-id survey-site-id site-name
                trap-station-longitude trap-station-latitude
                trap-station-altitude trap-station-notes
                trap-station-session-start-date primary-camera-id
                primary-camera-name primary-camera-status-id
                secondary-camera-id secondary-camera-name
                secondary-camera-status-id))

(s/defn tdeployment
  [{:keys [trap-station-session-id trap-station-name site-id trap-station-id
           trap-station-longitude trap-station-latitude trap-station-altitude
           trap-station-notes trap-station-session-start-date
           trap-station-session-end-date primary-camera-id primary-camera-name
           primary-camera-status-id secondary-camera-id secondary-camera-name
           secondary-camera-status-id]}]
  (->TDeployment trap-station-session-id trap-station-name site-id trap-station-id
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
   data]
  (db/with-db-keys state -set-camera-status! data))

(s/defn set-statuses-for-cameras!
  [state :- State
   data]
  (let [orig-data (get-specific state (:trap-station-session-id data))]
    (when (not= (:primary-camera-status-id data)
                (:primary-camera-status-id orig-data))
      (set-camera-status! state {:camera-status-id (:primary-camera-status-id data)
                                 :camera-id (:primary-camera-id orig-data)}))
    (when (not= (:secondary-camera-status-id data)
                (:secondary-camera-status-id orig-data))
      (set-camera-status! state {:camera-status-id (:secondary-camera-status-id data)
                                 :camera-id (:secondary-camera-id orig-data)}))))

(s/defn create-new-session!
  [state :- State
   data]
  (let [sdata (select-keys data [:trap-station-id :trap-station-session-start-date])
        s (trap-station-session/create!
           state (trap-station-session/ttrap-station-session sdata))]
    (when (:primary-camera-id data)
      (trap-station-session-camera/create!
       state
       (trap-station-session-camera/ttrap-station-session-camera
        {:trap-station-session-id (:trap-station-session-id s)
         :camera-id (:primary-camera-id data)})))
    (when (:secondary-camera-id data)
      (trap-station-session-camera/create!
       state
       (trap-station-session-camera/ttrap-station-session-camera
        {:trap-station-session-id (:trap-station-session-id s)
         :camera-id (:secondary-camera-id data)})))
    s))

(s/defn create-camera-check!
  [state :- State
   data :- TDeployment]
  (if (validate-camera-check state data)
    (db/with-transaction [s state]
      (set-session-end-date! s data)
      (set-statuses-for-cameras! s data)
      (create-new-session! s data))))
