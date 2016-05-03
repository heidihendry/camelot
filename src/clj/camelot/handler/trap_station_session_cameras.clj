(ns camelot.handler.trap-station-session-cameras
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station-session-camera :refer
             [TrapStationSessionCamera TrapStationSessionCameraCreate]]))

(sql/defqueries "sql/trap-station-session-cameras.sql" {:connection db/spec})

(s/defn get-all :- [TrapStationSessionCamera]
  [state id]
  (db/with-db-keys -get-all {:trap-station-session-id id}))

(s/defn get-specific :- TrapStationSessionCamera
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:trap-station-session-camera-id id})))

(s/defn create!
  [state
   data :- TrapStationSessionCameraCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- TrapStationSessionCamera]
  (db/with-db-keys -update! data)
  (get-specific state (:trap-station-session-camera-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-session-camera-id id}))

(s/defn get-available
  [state id]
  (db/with-db-keys -get-available {:trap-station-session-id id}))
