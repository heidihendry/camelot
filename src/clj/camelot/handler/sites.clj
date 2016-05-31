(ns camelot.handler.sites
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.site :refer [Site SiteCreate]]))

(sql/defqueries "sql/sites.sql" {:connection db/spec})

(s/defn get-all :- [Site]
  [state]
  (db/clj-keys (db/with-connection (:connection state) -get-all)))

(s/defn get-specific :- Site
  [state
   id :- s/Num]
  (first (db/with-db-keys state -get-specific {:site-id id})))

(s/defn get-specific-by-name
  [state
   data :- {:site-name s/Str}]
  (first (db/with-db-keys state -get-specific-by-name data)))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys state -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   id :- s/Num
   data :- Site]
  (db/with-db-keys state -update! (merge data {:site-id id}))
  (get-specific state (:site-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys state -delete! {:site-id id}))

(def routes
  (context "/sites" []
           (GET "/" [] (rest/list-resources get-all :site))
           (GET "/:id" [id] (rest/specific-resource get-specific id))
           (PUT "/:id" [id data] (rest/update-resource update! id data))
           (POST "/" [data] (rest/create-resource create! data))
           (DELETE "/:id" [id] (rest/delete-resource delete! id))))
