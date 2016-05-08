(ns camelot.handler.trap-station-session-cameras
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
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
   id :- s/Num
   data :- TrapStationSessionCamera]
  (db/with-db-keys -update! (merge data {:trap-station-session-camera-id id}))
  (get-specific state (:trap-station-session-camera-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-session-camera-id id}))

(s/defn get-available
  [state id]
  (db/with-db-keys -get-available {:trap-station-session-id id}))

(s/defn get-alternatives
  [state id]
  (let [res (get-specific state id)]
    (db/with-db-keys -get-alternatives res)))

(def routes
  (context "/trap-station-session-cameras" []
           (GET "/trap-station-session/:id" [id]
                (rest/list-resources get-all :trap-station-session-camera id))
           (GET "/available/:id" [id] (rest/list-available get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available get-alternatives id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
