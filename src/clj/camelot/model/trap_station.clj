(ns camelot.model.trap-station
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]))

(sql/defqueries "sql/trap-stations.sql" {:connection db/spec})

(defn valid-range?
  [rs re l]
  (or (nil? l) (and (>= l rs) (<= l re))))

(def valid-longitude? (partial valid-range? -180.0 180.0))
(def valid-latitude? (partial valid-range? -90.0 90.0))

(s/defrecord TTrapStation
    [trap-station-name :- s/Str
     survey-site-id :- s/Num
     trap-station-longitude :- (s/pred valid-longitude?)
     trap-station-latitude :- (s/pred valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)])

(s/defrecord TrapStation
    [trap-station-id :- s/Num
     trap-station-created :- org.joda.time.DateTime
     trap-station-updated :- org.joda.time.DateTime
     trap-station-name :- s/Str
     survey-site-id :- s/Num
     trap-station-longitude :- (s/pred valid-longitude?)
     trap-station-latitude :- (s/pred valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)])

(s/defn trap-station
  [{:keys [trap-station-id trap-station-created trap-station-updated
           trap-station-name survey-site-id trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-notes]}]
  (->TrapStation trap-station-id trap-station-created trap-station-updated
                 trap-station-name survey-site-id trap-station-longitude
                 trap-station-latitude trap-station-altitude
                 trap-station-notes))

(s/defn ttrap-station
  [{:keys [trap-station-name survey-site-id trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-notes]}]
  (->TTrapStation trap-station-name survey-site-id trap-station-longitude
                  trap-station-latitude trap-station-altitude
                  trap-station-notes))

(s/defn get-all :- [TrapStation]
  [state :- State
   id :- s/Int]
  (->> {:survey-site-id id}
       (db/with-db-keys state -get-all)
       (map trap-station)))

(s/defn get-specific :- (s/maybe TrapStation)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (trap-station)))

(s/defn get-specific-by-location :- (s/maybe TrapStation)
  [state :- State
   data :- TTrapStation]
  (some->> data
           (db/with-db-keys state -get-specific-by-location)
           (first)
           (trap-station)))

(s/defn create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (let [record (db/with-db-keys state -create<! data)]
    (trap-station (get-specific state (int (:1 record))))))

(s/defn update! :- TrapStation
  [state :- State
   id :- s/Int
   data :- TTrapStation]
  (db/with-db-keys state -update! (merge data {:trap-station-id id}))
  (trap-station (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:trap-station-id id}))

(s/defn get-or-create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (or (get-specific-by-location state data)
      (create! state data)))
