(ns camelot.model.species-mass
  "Species mass models and data access."
  (:require
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]))

(sql/defqueries "sql/species-mass.sql")

(defn- label-record
  "Add label to species record data."
  [data]
  (map #(assoc % :species-mass-label
               (str (:species-mass-start %) "-"
                    (:species-mass-end %) " kg"))
       data))

(s/defrecord SpeciesMass
    [species-mass-id :- s/Num
     species-mass-start :- s/Int
     species-mass-end :- s/Int
     species-mass-label :- s/Str]
  {s/Any s/Any})

(def species-mass map->SpeciesMass)

(s/defn get-all :- [SpeciesMass]
  "Retrieve, translate and return all species mass brackets."
  [state :- State]
  (->> (db/with-connection state -get-all)
       (db/clj-keys)
       label-record
       (map species-mass)))
