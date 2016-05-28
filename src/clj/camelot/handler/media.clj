(ns camelot.handler.media
  (:require [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/media.sql" {:connection db/spec})

(s/defn get-all
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:media-id id})))

(s/defn create!
  [state data]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))
