(ns camelot.library.query.fields
  (:require [camelot.library.query.sighting-fields :as sighting-fields]
            [camelot.library.query.util :as qutil]
            [camelot.util.model :as model]
            [camelot.util.search :as search-util]))

(def ^:private taxonomy-label-column
  [:concat [:concat :taxonomy.taxonomy-genus [:cast " " :char]]
   :taxonomy.taxonomy-species])

(defn- computed-column
  [k]
  (or
   (when (or (= k :taxonomy-label) (= k :species))
     taxonomy-label-column)
   (get search-util/field-keys k)))

(defn field->column
  [f]
  (let [fk (keyword f)
        m (get model/schema-definitions fk)]
    (if-let [table (:table m)]
      (keyword (qutil/->column table fk))
      (or (computed-column fk)
          (sighting-fields/sighting-field-column fk)
          (throw (ex-info "Unknown field" {:field fk}))))))

(defn full-text-fields
  []
  (conj (model/qualified-searchable-field-keys)
        :sighting-field-value.sighting-field-value-data
        taxonomy-label-column))

(def base-field-datatypes
  (reduce-kv
   (fn [acc k v]
     (assoc acc (name k) (select-keys v [:datatype])))
   {}
   model/schema-definitions))

(def computed-field-datatypes
  (reduce-kv
   (fn [acc k v]
     (assoc acc (name k) (get base-field-datatypes (name v))))
   {}
   search-util/field-keys))

(defn field-datatypes
  [state]
  (merge (sighting-fields/sighting-field-datatypes state)
         computed-field-datatypes
         base-field-datatypes))

(defn auto-coalesce-field
  [dt fc]
  (condp = dt
    :boolean
    [:coalesce fc false]

    fc))
