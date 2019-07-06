(ns camelot.util.sighting-fields
  (:require [clojure.string :as cstr]))

(def datatypes
  {:text
   {:deserialiser-datatype :string
    :translation-key :datatype/text}

   :textarea
   {:deserialiser-datatype :string
    :translation-key :datatype/textarea}

   :number
   {:deserialiser-datatype :number
    :translation-key :datatype/number}

   :select
   {:deserialiser-datatype :string
    :translation-key :datatype/select
    :has-options true}

   :checkbox
   {:deserialiser-datatype :boolean
    :translation-key :datatype/checkbox}})

(def user-key-prefix "field-")
(def user-key-re-pattern (re-pattern (str "^" user-key-prefix)))

(defn key->name
  [sfk]
  (cstr/replace (name sfk) user-key-re-pattern ""))

(defn user-key
  [sighting-field]
  (keyword (str user-key-prefix (if (map? sighting-field)
                                  (:sighting-field-key sighting-field)
                                  sighting-field))))
