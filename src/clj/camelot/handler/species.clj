(ns camelot.handler.species
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/species.sql" {:connection db/spec})

(s/defn get-all
  [state]
  (db/clj-keys (db/with-connection (:connection state) -get-all)))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:species-id id})))

(s/defn get-specific-by-scientific-name
  [state
   scientific-name :- s/Str]
  (first (db/with-db-keys state -get-specific-by-scientific-name
           {:species-scientific-name scientific-name})))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data]
  (db/with-db-keys state -update! (merge data {:species-id id}))
  (get-specific state (:species-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:species-id id}))

(def routes
  (context "/species" []
           (GET "/" [] (rest/list-resources get-all :species))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
