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
  (db/clj-keys (-get-all {:media-id id})))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:sighting-id id})))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))
