(ns camelot.handler.photos
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/photos.sql" {:connection db/spec})

(s/defn get-all
  [state
   id :- s/Num]
  (db/with-db-keys state -get-all {:media-id id}))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:photo-id id})))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data]
  (db/with-db-keys state -update! (merge data {:photo-id id}))
  (get-specific state (:photo-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:photo-id id}))

(def routes
  (context "/photos" []
           (GET "/media/:id" [id] (rest/list-resources get-all :photo id))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
