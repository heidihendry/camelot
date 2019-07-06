(ns camelot.library.query.sighting-fields
  (:require [camelot.library.query.util :as qutil]
            [camelot.model.sighting-field :as sighting-field]
            [camelot.util.sighting-fields :as sfutil]))

(defn- sighting-field?
  [fk]
  (re-matches #"^field-.*" (name fk)))

(defn field-keys
  [pt]
  (->> pt
       qutil/field-names
       (filter sighting-field?)
       (map keyword)))

(defn- field-value-alias
  [sfk]
  (keyword (str (name sfk) "-value")))

(defn sighting-field-column
  [fk]
  (when (sighting-field? fk)
    (qutil/->column (field-value-alias fk) :sighting-field-value-data)))

(defn- join
  [sfk]
  (let [sfva (field-value-alias sfk)]
    [[:sighting-field sfk]
     [:and [:= (qutil/->column sfk :survey-id) :survey.survey-id]
      [:= (qutil/->column sfk :sighting-field-key) (sfutil/key->name sfk)]]

     [:sighting-field-value sfva]
     [:and [:= (qutil/->column sfva :sighting-id) :sighting.sighting-id]
      [:= (qutil/->column sfva :sighting-field-id) (qutil/->column sfk :sighting-field-id)]]]))

(defn- joins
  [pt]
  (let [sfks (field-keys pt)]
    (vec (mapcat join sfks))))

(defn join-fields
  [pt sqlq]
  (update sqlq :left-join concat (joins pt)))

(defn- sighting-field-deserialiser
  [sf]
  (-> sf
      :sighting-field-datatype
      sfutil/datatypes
      :deserialiser-datatype))

(defn sighting-field-datatypes
  [state]
  (reduce (fn [acc sf]
            (assoc acc
                   (name (sfutil/user-key sf))
                   {:datatype (sighting-field-deserialiser sf)}))
          {} (sighting-field/get-all state)))
