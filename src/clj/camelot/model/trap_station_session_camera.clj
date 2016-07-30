(ns camelot.model.trap-station-session-camera
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station-session :as trap-station-session]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]))

(sql/defqueries "sql/trap-station-session-cameras.sql" {:connection db/spec})

(s/defrecord TrapStationSessionCamera
    [trap-station-session-camera-id :- s/Int
     trap-station-session-camera-created :- org.joda.time.DateTime
     trap-station-session-camera-updated :- org.joda.time.DateTime
     camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-session-camera-import-path :- (s/maybe s/Str)
     camera-name :- (s/maybe s/Str)])

(s/defrecord TTrapStationSessionCamera
    [camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-session-camera-import-path :- (s/maybe s/Str)])

(s/defn trap-station-session-camera :- TrapStationSessionCamera
  [{:keys [trap-station-session-camera-id
           trap-station-session-camera-created
           trap-station-session-camera-updated
           camera-id
           trap-station-session-id
           trap-station-session-camera-import-path
           camera-name]}]
  (->TrapStationSessionCamera trap-station-session-camera-id
                              trap-station-session-camera-created
                              trap-station-session-camera-updated
                              camera-id
                              trap-station-session-id
                              trap-station-session-camera-import-path
                              camera-name))

(s/defn ttrap-station-session-camera :- TTrapStationSessionCamera
  [{:keys [camera-id
           trap-station-session-id
           trap-station-session-camera-import-path]}]
  (->TTrapStationSessionCamera camera-id
                               trap-station-session-id
                               trap-station-session-camera-import-path))



(s/defn get-all :- [TrapStationSessionCamera]
  [state :- State
   id :- s/Int]
  (->> {:trap-station-session-id id}
       (db/with-db-keys state -get-all)
       (map trap-station-session-camera)))

(s/defn get-specific :- (s/maybe TrapStationSessionCamera)
  [state :- State
   id :- s/Num]
  (some->> {:trap-station-session-camera-id id}
           (db/with-db-keys state -get-specific )
           (first)
           (trap-station-session-camera)))

(s/defn get-specific-by-import-path :- (s/maybe TrapStationSessionCamera)
  [state :- State
   path :- s/Str]
  (some->> {:trap-station-session-camera-import-path path}
           (db/with-db-keys state -get-specific-by-import-path)
           (first)
           (trap-station-session-camera)))

(defn- camera-available?
  [state data]
  (not (some #(= % (:camera-id data))
             (trap-station-session/get-active
              state (int (:trap-station-session-id data))))))

(s/defn create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  {:pre [(camera-available? state data)]}
  (let [record (db/with-db-keys state -create<! data)]
    (trap-station-session-camera (get-specific state (int (:1 record))))))

(defn- camera-available-for-update?
  [state id data]
  (not (some #(= % (:camera-id data))
             (trap-station-session/get-active state
                         (int (:trap-station-session-id data))
                         id))))

(s/defn update! :- TrapStationSessionCamera
  [state :- State
   id :- s/Int
   data :- TTrapStationSessionCamera]
  {:pre [(camera-available-for-update? state id data)]}
  (db/with-db-keys state -update!
    (merge data {:trap-station-session-camera-id id}))
  (trap-station-session-camera (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -delete! {:trap-station-session-camera-id id}))

(s/defn get-available
  "Return the available cameras, factoring in whether they're in use elsewhere."
  [state :- State
   id :- s/Int]
  (db/clj-keys (-get-available)))

(s/defn get-alternatives
  "Return the current and alternative cameras, factoring in whether they're in
  use elsewhere."
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (->> res
         (db/with-db-keys state -get-alternatives))))

(s/defn get-or-create! :- TrapStationSessionCamera
  [state :- State
   data :- TTrapStationSessionCamera]
  (or (get-specific-by-import-path
       state (:trap-station-session-camera-import-path data))
      (create! state data)))
