(ns camelot.model.sighting-field-value
  "User-defined fields for sighting data."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.datatype :as datatype]
   [clojure.string :as str]
   [camelot.model.sighting-field :as sighting-field]
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
          user-key (util.sf/user-key data)]
      (assoc ddata user-key (:sighting-field-value-data ddata)))))

(defn sighting-field-value
  "Return a record, deserialising the field value data in the process."
  [data]
  (map->SightingFieldValue (augment-data data)))

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

(defn- survey-fields-by-key
  [state survey-id]
  (reduce-kv (fn [acc k v] (assoc acc k (first v))) {}
          (group-by :sighting-field-key
                    (filter #(= survey-id (:survey-id %))
                            (sighting-field/get-all state)))))

(defn create-bulk!
  "Create sighting field values from user-field/value pairs."
  [state sighting-id survey-id data]
  (let [survey-sf (survey-fields-by-key state survey-id)
        user-key-re (re-pattern (str "^" util.sf/user-key-prefix))]
    (dorun (->> data
                (filter (fn [[k v]] (re-find user-key-re (name k))))
                (map
                 (fn [[k v]]
                   (let [sf (get survey-sf
                                 (str/replace (name k) user-key-re ""))]
                     [(:sighting-field-id sf) v])))
                (filter (fn [[k v]] (not (nil? k))))
                (map (fn [[k v]] (create! state sighting-id k v)))))))

(defn update!
  [state id value]
  (db/with-db-keys state -update! {:sighting-field-value-id id
                                   :sighting-field-value-data (str value)})
  (get-specific state id))

(defn get-for-sighting
  "Get field data for the given sighting."
  [state sighting-id]
  (db/with-db-keys state -get-for-sighting {:sighting-id sighting-id}))

(defn update-for-sighting!
  "Given a map of {field-id value-data}, create or update entries for a sighting."
  [state sighting-id new-field-data]
  (let [cur-field-data (reduce #(assoc %1 (:sighting-field-id %2) %2)
                               {} (get-for-sighting state sighting-id))]
    (doseq [[k v] (vec new-field-data)]
      (let [curdata (get cur-field-data k)
            curval (:sighting-field-value-data curdata)]
        (if (nil? curval)
          (create! state sighting-id k v)
          (when (not= v curval)
            (update! state (:sighting-field-value-id curdata) v)))))))

(defn delete-for-sighting!
  "Delete sighting field values for the given sighting ID."
  [state sighting-id]
  (db/with-db-keys state -delete-for-sighting! {:sighting-id sighting-id})
  nil)
