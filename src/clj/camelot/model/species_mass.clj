(ns camelot.model.species-mass
  "Species mass models and data access."
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]))

(def query (db/with-db-keys :species-mass))

(defn- label-record
  "Add label to species record data."
  [data]
  (map #(assoc % :species-mass-label
               (str (:species-mass-start %) "-"
                    (:species-mass-end %) " kg"))
       data))

(sch/defrecord SpeciesMass
    [species-mass-id :- sch/Num
     species-mass-start :- sch/Int
     species-mass-end :- sch/Int
     species-mass-label :- sch/Str]
  {sch/Any sch/Any})

(def species-mass map->SpeciesMass)

(sch/defn get-all :- [SpeciesMass]
  "Retrieve, translate and return all species mass brackets."
  [state :- State]
  (->> (query state :get-all)
       label-record
       (map species-mass)))
