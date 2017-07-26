(ns camelot.model.trap-station
  (:require
   [schema.core :as s]
   [camelot.system.state :refer [State]]
   [camelot.util.trap-station :as util.ts]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :trap-stations))

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
     trap-station-distance-to-settlement :- (s/maybe s/Num)]
  {s/Any s/Any})

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
     trap-station-distance-to-settlement :- (s/maybe s/Num)]
  {s/Any s/Any})

(defn- round-gps
  "Round GPS coordinates to 6dp (accurate to 1 meter)."
  [coord]
  (Double/parseDouble (format "%.6f" (double coord))))

(def trap-station map->TrapStation)

(defn ttrap-station
  "Create TTrapStation, rounding GPS coordinates"
  [data]
  (map->TTrapStation
   (-> data
       (update :trap-station-latitude round-gps)
       (update :trap-station-longitude round-gps))))

(s/defn get-all :- [TrapStation]
  [state :- State
   id :- s/Int]
  (->> {:survey-site-id id}
       (query state :get-all)
       (map trap-station)))

(s/defn get-all* :- [TrapStation]
  [state :- State]
  (map trap-station (query state :get-all*)))

(s/defn get-all-for-survey :- [TrapStation]
  [state :- State
   survey-id :- s/Int]
  (map trap-station (query state :get-all-for-survey {:survey-id survey-id})))

(s/defn get-specific :- (s/maybe TrapStation)
  [state :- State
   id :- s/Int]
  (some->> {:trap-station-id id}
           (query state :get-specific)
           (first)
           (trap-station)))

(s/defn get-specific-by-location :- (s/maybe TrapStation)
  [state :- State
   data :- TTrapStation]
  (some->> data
           (query state :get-specific-by-location)
           (first)
           (trap-station)))

(s/defn create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (let [record (query state :create<! data)]
    (trap-station (get-specific state (int (:1 record))))))

(s/defn update! :- TrapStation
  [state :- State
   id :- s/Int
   data :- TTrapStation]
  (query state :update! (merge data {:trap-station-id id}))
  (trap-station (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-trap-station state id)
        ps {:trap-station-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/defn get-or-create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (or (get-specific-by-location state data)
      (create! state data)))
