(ns camelot.handler.sites
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.site :refer [Site SiteCreate]]))

(sql/defqueries "sql/sites.sql" {:connection db/spec})

(s/defn get-all :- [Site]
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific :- Site
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:site-id id})))

(s/defn get-specific-by-name :- Site
  [state
   data :- {:site-name s/Str}]
  (db/with-db-keys -get-specific-by-name data))

(s/defn create!
  [state
   data :- SiteCreate]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))

(s/defn update!
  [state
   data :- Site]
  (db/with-db-keys -update! data)
  (get-specific state (:site-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:site-id id}))
