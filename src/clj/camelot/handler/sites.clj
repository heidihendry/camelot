(ns camelot.handler.sites
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.site :refer [Site SiteCreate SiteUpdate]]))

(sql/defqueries "sql/sites.sql" {:connection db/spec})

(s/defn get-all :- [Site]
  [state]
  (-get-all))

(s/defn get-specific :- Site
  [state id]
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
   data :- SiteUpdate]
  (db/with-db-keys -update! data)
  (get-specific state (:site-id data)))

(s/defn delete!
  [state
   id :- s/Num]
  (db/with-db-keys -delete! {:site-id id}))
