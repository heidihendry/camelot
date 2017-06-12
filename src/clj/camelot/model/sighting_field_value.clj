(ns camelot.model.sighting-field-value
  "User-defined fields for sighting data."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.datatype :as datatype]
   [camelot.util.sighting-fields :as util.sf]
   [camelot.util.db :as db]
   [clj-time.core :as t]))

(sql/defqueries "sql/sighting-field-value.sql")

(s/defrecord SightingFieldValue
    [sighting-field-value-id :- s/Int
     sighting-field-value-created :- org.joda.time.DateTime
     sighting-field-value-updated :- org.joda.time.DateTime
     sighting-field-value-data :- s/Any
     sighting-field-id :- s/Int
     sighting-id :- s/Int]
  {s/Any s/Any})

(defn- deserialiser-datatype
  "Return the datatype for the deserialiser."
  [data]
  (->> data
       :sighting-field-datatype
       keyword
       util.sf/datatypes
       :deserialiser-datatype))

(defn augment-data
  [data]
  (letfn [(deserialise [v] (datatype/deserialise (deserialiser-datatype data) v))]
    (let [ddata (update data :sighting-field-value-data deserialise)
          user-key (keyword (str "field-" (:sighting-field-key data)))]
      (assoc ddata user-key (:sighting-field-value-data ddata)))))

(defn sighting-field-value
  "Return a record, deserialising the field value data in the process."
  [data]
  (map->SightingFieldValue (augment-data data)))

(defn get-all
  "Return all sighting field values for a collection of sighting IDs."
  [state sighting-ids]
  (->> {:sighting-ids sighting-ids}
       (db/with-db-keys state -get-all)
       (map sighting-field-value)))

(defn sighting-field-query-reducer
  [acc sighting-id fields]
  (->> fields
       (map augment-data)
       (apply merge)
       ((fn [x] (dissoc x :sighting-field-key
                        :sighting-field-value-data
                        :sighting-field-datatype)))
       (assoc acc sighting-id)))

(defn query-all
  "Return all sighting field values."
  [state]
  (->> (db/with-db-keys state -query-all {})
       (group-by :sighting-id)
       (reduce-kv sighting-field-query-reducer {})))

(defn get-specific
  "Return a specific sighting field value."
  [state value-id]
  (->> {:sighting-field-value-id value-id}
       (db/with-db-keys state -get-specific)
       first
       sighting-field-value))

(defn create!
  "Create a new sighting field value."
  [state sighting-id field-id value]
  (let [record (db/with-db-keys state -create<! {:sighting-field-id field-id
                                                 :sighting-field-value-data (str value)
                                                 :sighting-id sighting-id})]
    (get-specific state (int (:1 record)))))

(defn update!
  [state id value]
  (db/with-db-keys state -update! {:sighting-field-value-id id
                                   :sighting-field-value-data (str value)})
  (get-specific state id))

(defn delete-for-sighting!
  "Delete sighting field values for the given sighting ID."
  [state sighting-id]
  (db/with-db-keys state -delete-for-sighting! {:sighting-id sighting-id})
  nil)
