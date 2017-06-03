(ns camelot.model.sighting-field
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]
   [clj-time.core :as t]))

(sql/defqueries "sql/sighting-field.sql")

(s/defrecord TSightingField
    [sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Str
     sighting-field-required :- s/Bool
     sighting-field-default :- s/Str
     sighting-field-affects-independence :- s/Bool
     sighting-field-ordering :- s/Int
     survey-id :- s/Int]
  {s/Any s/Any})

(s/defrecord SightingField
    [sighting-field-id :- s/Int
     sighting-field-created :- org.joda.time.DateTime
     sighting-field-updated :- org.joda.time.DateTime
     sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Str
     sighting-field-default :- (s/maybe s/Str)
     sighting-field-required :- s/Bool
     sighting-field-affects-independence :- s/Bool
     sighting-field-ordering :- s/Int
     survey-id :- s/Int]
  {s/Any s/Any})

(def sighting-field map->SightingField)
(def tsighting-field map->TSightingField)

(defn get-all
  [state]
  (map sighting-field (db/with-db-keys state -get-all {})))

(defn get-specific
  [state field-id]
  (sighting-field (first (db/with-db-keys state -get-specific {:sighting-field-id field-id}))))

(defn update!
  [state id field-config]
  (db/with-db-keys state -update! (assoc field-config :sighting-field-id id))
  (get-specific state id))

(defn create!
  [state field-config]
  (let [record (db/with-db-keys state -create<! field-config)]
    (get-specific state (int (:1 record)))))

(defn delete!
  [state field-id]
  (db/with-db-keys state -delete! {:sighting-field-id field-id})
  nil)

(defn create-option!
  [state field-id option-config]
  (db/with-db-keys state -create-option<!
    (assoc option-config :sighting-field-id field-id)))
