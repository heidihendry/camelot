(ns camelot.model.trap-station
  (:require
   [schema.core :as sch]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.trap-station :as utilts]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera])
  (:import
   (java.util Locale)
   (java.lang String)))

(def query (db/with-db-keys :trap-stations))

(sch/defrecord TTrapStation
    [trap-station-name :- sch/Str
     survey-site-id :- sch/Num
     trap-station-longitude :- (sch/pred utilts/valid-longitude?)
     trap-station-latitude :- (sch/pred utilts/valid-latitude?)
     trap-station-altitude :- (sch/maybe sch/Num)
     trap-station-notes :- (sch/maybe sch/Str)
     trap-station-distance-above-ground :- (sch/maybe sch/Num)
     trap-station-distance-to-road :- (sch/maybe sch/Num)
     trap-station-distance-to-river :- (sch/maybe sch/Num)
     trap-station-distance-to-settlement :- (sch/maybe sch/Num)]
  {sch/Any sch/Any})

(sch/defrecord TrapStation
    [trap-station-id :- sch/Num
     trap-station-created :- org.joda.time.DateTime
     trap-station-updated :- org.joda.time.DateTime
     trap-station-name :- sch/Str
     survey-site-id :- sch/Num
     trap-station-longitude :- (sch/pred utilts/valid-longitude?)
     trap-station-latitude :- (sch/pred utilts/valid-latitude?)
     trap-station-altitude :- (sch/maybe sch/Num)
     trap-station-notes :- (sch/maybe sch/Str)
     trap-station-distance-above-ground :- (sch/maybe sch/Num)
     trap-station-distance-to-road :- (sch/maybe sch/Num)
     trap-station-distance-to-river :- (sch/maybe sch/Num)
     trap-station-distance-to-settlement :- (sch/maybe sch/Num)]
  {sch/Any sch/Any})

(defn round-gps
  "Round GPS coordinates to 6dp (accurate to 1 meter)."
  [coord]
  (->> [(double coord)]
       (into-array Object)
       (String/format Locale/ROOT "%.6f")
       (Double/parseDouble)))

(def trap-station map->TrapStation)

(defn ttrap-station
  "Create TTrapStation, rounding GPS coordinates"
  [data]
  (map->TTrapStation
   (-> data
       (update :trap-station-latitude round-gps)
       (update :trap-station-longitude round-gps))))

(sch/defn get-all :- [TrapStation]
  [state :- State
   id :- sch/Int]
  (->> {:survey-site-id id}
       (query state :get-all)
       (map trap-station)))

(sch/defn get-all* :- [TrapStation]
  [state :- State]
  (map trap-station (query state :get-all*)))

(sch/defn get-all-for-survey :- [TrapStation]
  [state :- State
   survey-id :- sch/Int]
  (map trap-station (query state :get-all-for-survey {:survey-id survey-id})))

(sch/defn get-specific :- (sch/maybe TrapStation)
  [state :- State
   id :- sch/Int]
  (some->> {:trap-station-id id}
           (query state :get-specific)
           (first)
           (trap-station)))

(sch/defn get-specific-by-name-and-location :- (sch/maybe TrapStation)
  [state :- State
   data :- TTrapStation]
  (some->> data
           (query state :get-specific-by-name-and-location)
           (first)
           (trap-station)))

(sch/defn create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (let [record (query state :create<! data)]
    (trap-station (get-specific state (int (:1 record))))))

(sch/defn update! :- TrapStation
  [state :- State
   id :- sch/Int
   data :- TTrapStation]
  (query state :update! (merge data {:trap-station-id id}))
  (trap-station (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-trap-station state id)
        ps {:trap-station-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(sch/defn get-or-create! :- TrapStation
  [state :- State
   data :- TTrapStation]
  (or (get-specific-by-name-and-location state data)
      (create! state data)))
