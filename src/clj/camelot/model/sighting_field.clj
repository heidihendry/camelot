(ns camelot.model.sighting-field
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]))

(sql/defqueries "sql/sighting-field.sql")

(s/defrecord TSightingField
    [sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Str]
  {s/Any s/Any})

(s/defrecord SightingField
    [sighting-field-id :- s/Int
     sighting-field-created :- org.joda.time.DateTime
     sighting-field-updated :- org.joda.time.DateTime
     sighting-field-key :- s/Str
     sighting-field-label :- s/Str
     sighting-field-datatype :- s/Str]
  {s/Any s/Any})

(def sighting-field map->SightingField)
(def tsighting-field map->TSightingField)

(defn get-all
  [state]
  (map sighting-field (db/with-db-keys state -get-all {})))

(defn get-specific
  [state field-id]
  (sighting-field (db/with-db-keys state -get-specific {:sighting-field-id field-id})))

(defn update-label!
  [state {:keys [label field-id]}]
  ;; TODO changing label should update all values using it.
  (db/with-db-keys state -update-label!
    {:sighting-field-id field-id
     :sighting-field-label label})
  (get-specific state field-id))

(defn create!
  [state field-config]
  (let [record (db/with-db-keys state -create<! field-config)]
    (get-specific state (int (:1 record)))))

(defn delete!
  [state field-id]
  (db/with-db-keys state -delete! {:sighting-field-id field-id})
  nil)

(defn get-all-options
  [state sighting-field-id]
  (db/with-db-keys state -get-all-options {:sighting-field-id sighting-field-id}))

(defn update-option-visibility!
  [state visible? field-option-id]
  (db/with-db-keys state -update-option-visibility!
    {:sighting-field-option-id field-option-id
     :sighting-field-option-visible visible?}))

(defn update-option-label!
  [state option-label field-option-id]
  (db/with-db-keys state -update-option-label!
    {:sighting-field-option-id field-option-id
     :sighting-field-option-label option-label}))

(defn create-option!
  [state field-id option-config]
  (db/with-db-keys state -create-option<!
    (assoc option-config :sighting-field-id field-id)))
