(ns camelot.handler.species
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.util.rest :as rest]
            [camelot.db :as db]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/species.sql" {:connection db/spec})

(s/defn get-all
  [state]
  (db/clj-keys (-get-all)))

(s/defn get-specific
  [state
   id :- s/Num]
  (first (db/with-db-keys -get-specific {:species-id id})))

(s/defn get-specific-by-scientific-name
  [state
   scientific-name :- s/Str]
  (first (db/with-db-keys -get-specific-by-scientific-name
           {:species-scientific-name scientific-name})))

(s/defn create!
  [state
   data]
  (let [record (db/with-db-keys -create<! data)]
    (get-specific state (:1 record))))
