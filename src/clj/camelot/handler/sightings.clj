(ns camelot.handler.sightings
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/sightings.sql" {:connection db/spec})

(s/defn get-all
  [state
   id :- s/Num]
  (db/with-db-keys state -get-all {:media-id id}))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:sighting-id id})))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data]
  (db/with-db-keys state -update! (merge data {:sighting-id id}))
  (get-specific state (:sighting-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:sighting-id id}))

(s/defn get-available
  [state id]
  (db/with-db-keys state -get-available {:sighting-id id}))

(s/defn get-alternatives
  [state id]
  (let [res (get-specific state id)]
    (db/with-db-keys state -get-alternatives res)))

(def routes
  (context "/sightings" []
           (GET "/media/:id" [id] (rest/list-resources get-all :sighting id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (GET "/available/:id" [id] (rest/list-available get-available id))
           (GET "/alternatives/:id" [id] (rest/list-available get-alternatives id))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
