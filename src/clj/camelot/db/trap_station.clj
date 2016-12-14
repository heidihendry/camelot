(ns camelot.db.trap-station
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.app.state :refer [State]]
   [camelot.util.trap-station :as util.ts]
   [camelot.util.db :as db]
   [camelot.db.media :as media]))

(sql/defqueries "sql/trap-stations.sql")

(s/defrecord TTrapStation
    [trap-station-name :- s/Str
     survey-site-id :- s/Num
     trap-station-longitude :- (s/pred util.ts/valid-longitude?)
     trap-station-latitude :- (s/pred util.ts/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)])

(s/defrecord TrapStation
    [trap-station-id :- s/Num
     trap-station-created :- org.joda.time.DateTime
     trap-station-updated :- org.joda.time.DateTime
     trap-station-name :- s/Str
     survey-site-id :- s/Num
     trap-station-longitude :- (s/pred util.ts/valid-longitude?)
     trap-station-latitude :- (s/pred util.ts/valid-latitude?)
     trap-station-altitude :- (s/maybe s/Num)
     trap-station-notes :- (s/maybe s/Str)
     trap-station-distance-above-ground :- (s/maybe s/Num)
     trap-station-distance-to-road :- (s/maybe s/Num)
     trap-station-distance-to-river :- (s/maybe s/Num)
     trap-station-distance-to-settlement :- (s/maybe s/Num)])

(s/defn trap-station
  [{:keys [trap-station-id trap-station-created trap-station-updated
           trap-station-name survey-site-id trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-notes trap-station-distance-above-ground
           trap-station-distance-to-road trap-station-distance-to-river
           trap-station-distance-to-settlement]}]
  (->TrapStation trap-station-id trap-station-created trap-station-updated
                 trap-station-name survey-site-id trap-station-longitude
                 trap-station-latitude trap-station-altitude
                 trap-station-notes trap-station-distance-above-ground
                 trap-station-distance-to-road trap-station-distance-to-river
                 trap-station-distance-to-settlement))

(s/defn ttrap-station
  [{:keys [trap-station-name survey-site-id trap-station-longitude
           trap-station-latitude trap-station-altitude
           trap-station-notes trap-station-distance-above-ground
           trap-station-distance-to-road trap-station-distance-to-river
           trap-station-distance-to-settlement]}]
  (->TTrapStation trap-station-name survey-site-id trap-station-longitude
                  trap-station-latitude trap-station-altitude
                  trap-station-notes trap-station-distance-above-ground
                  trap-station-distance-to-road trap-station-distance-to-river
                  trap-station-distance-to-settlement))

(s/defn get-all :- [TrapStation]
  [state :- State
   id :- s/Int]
  (->> {:survey-site-id id}
       (db/with-db-keys state -get-all)
       (map trap-station)))

(s/defn get-all* :- [TrapStation]
  [state :- State]
  (map trap-station (db/clj-keys (db/with-connection state -get-all*))))

(s/defn get-all-for-survey :- [TrapStation]
  [state :- State
   survey-id :- s/Int]
  (map trap-station (db/with-db-keys state -get-all-for-survey {:survey-id survey-id})))

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
  (let [fs (media/get-all-files-by-trap-station state id)]
    (db/with-db-keys state -delete! {:trap-station-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-or-create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (or (get-specific-by-location state data)
      (create! state data)))
