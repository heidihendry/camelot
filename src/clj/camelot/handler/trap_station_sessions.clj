(ns camelot.handler.trap-station-sessions
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
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
   id :- s/Num
   data :- TrapStationSession]
  (db/with-db-keys -update! (merge data {:trap-station-session-id id}))
  (get-specific state (:trap-station-session-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:trap-station-session-id id}))

(def routes
  (context "/trap-station-sessions" []
           (GET "/trap-station/:id" [id]
                (rest/list-resources get-all :trap-station-session id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
