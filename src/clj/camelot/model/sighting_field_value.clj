(ns camelot.model.sighting-field-value
  "User-defined fields for sighting data."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]
   [clj-time.core :as t]))

(sql/defqueries "sql/sighting-field-value.sql")

(defn get-all
  "Return all sighting field values for a collection of sighting IDs."
  [state sighting-ids]
  (db/with-db-keys state -get-all {:sighting-ids sighting-ids}))

(defn get-specific
  "Return a specific sighting field value."
  [state value-id]
  (->> {:sighting-field-value-id field-id}
       (db/with-db-keys state -get-specific)
       first))

(defn create!
  "Create a new sighting field value."
  [state sighting-id field-id value]
  (let [record (db/with-db-keys state -create<! {:sighting-field-id field-id
                                                 :sighting-field-value-data value
                                                 :sighting-id sighting-id})]
    (get-specific state (int (:1 record)))))

(defn update!
  [state id value]
  (db/with-db-keys state -update! {:sighting-field-value-id id
                                   :sighting-field-value-data value})
  (get-specific state id))

(defn delete-for-sighting!
  "Delete sighting field values for the given sighting ID."
  [state sighting-id]
  (db/with-db-keys state -delete-for-sighting! {:sighting-id sighting-id})
  nil)
