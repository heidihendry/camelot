(ns camelot.handler.trap-stations
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.trap-station :refer [TrapStation TrapStationCreate]]))

(sql/defqueries "sql/trap-stations.sql" {:connection db/spec})

(s/defn get-all :- [TrapStation]
  [state id]
  (db/with-db-keys state -get-all {:survey-site-id id}))

(s/defn get-specific :- TrapStation
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:trap-station-id id})))

(s/defn get-specific-by-location :- (s/maybe TrapStation)
  [state
   data]
  (first (db/with-db-keys state -get-specific-by-location data)))

(s/defn create!
  [state
   data :- TrapStationCreate]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data :- TrapStation]
  (db/with-db-keys state -update! (merge data {:trap-station-id id}))
  (get-specific state (:trap-station-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:trap-station-id id}))

(def routes
  (context "/trap-stations" []
           (GET "/site/:id" [id]
                (rest/list-resources get-all :trap-station id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
