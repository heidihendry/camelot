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
     sighting-field-datatype :- s/Keyword
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
     sighting-field-datatype :- s/Keyword
     sighting-field-default :- (s/maybe s/Str)
     sighting-field-required :- s/Bool
     sighting-field-affects-independence :- s/Bool
     sighting-field-ordering :- s/Int
     survey-id :- s/Int]
  {s/Any s/Any})

(defn sighting-field
  [data]
  (map->SightingField (update data :sighting-field-datatype keyword)))

(def tsighting-field map->TSightingField)

(defn get-all
  "Get all sighting fields."
  [state]
  (map sighting-field (db/with-db-keys state -get-all {})))

(defn get-with-media-ids
  "Get all sighting fields for media-ids, by way of values defined for their sightings."
  [state media-ids]
  (map sighting-field
       (db/with-db-keys state -get-with-media-ids {:media-ids media-ids})))

(defn get-specific
  "Return a specific sighting field by field ID."
  [state field-id]
  (->> {:sighting-field-id field-id}
       (db/with-db-keys state -get-specific)
       first
       sighting-field))

(defn update!
  "Update the sighting field with the given ID with `field-config'."
  [state id field-config]
  (db/with-db-keys state -update!
    (assoc (update field-config :sighting-field-datatype name)
           :sighting-field-id id))
  (get-specific state id))

(defn create!
  "Create a sighting field with its configuration as `field-config'."
  [state field-config]
  (->> (update field-config :sighting-field-datatype name)
       (db/with-db-keys state -create<! )
       :1
       int
       (get-specific state)))

(defn delete!
  "Delete the sighting field with the given ID."
  [state field-id]
  (db/with-db-keys state -delete! {:sighting-field-id field-id})
  nil)

(defn create-option!
  [state field-id option-config]
  (db/with-db-keys state -create-option<!
    (assoc option-config :sighting-field-id field-id)))
