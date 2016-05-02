(ns camelot.handler.trap-station-sessions
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station-session :refer
             [TrapStationSession TrapStationSessionCreate]]))

(sql/defqueries "sql/trap-station-sessions.sql" {:connection db/spec})

(s/defn get-all :- [TrapStationSession]
  [state id]
  (db/with-db-keys -get-all {:trap-station-id id}))

(s/defn get-specific :- TrapStationSession
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:trap-station-session-id id})))

(s/defn create!
  [state
   data :- TrapStationSessionCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- TrapStationSession]
  (db/with-db-keys -update! data)
  (get-specific state (:trap-station-session-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-session-id id}))
