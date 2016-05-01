(ns camelot.handler.trap-stations
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station :refer [TrapStation TrapStationCreate]]))

(sql/defqueries "sql/trap-stations.sql" {:connection db/spec})

(s/defn get-all :- [TrapStation]
  [state id]
  (db/with-db-keys -get-all {:survey-site-id id}))

(s/defn get-specific :- TrapStation
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:trap-station-id id})))

(s/defn create!
  [state
   data :- TrapStationCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- TrapStation]
  (db/with-db-keys -update! data)
  (get-specific state (:trap-station-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-id id}))
