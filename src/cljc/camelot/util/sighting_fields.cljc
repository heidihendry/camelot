(ns camelot.util.sighting-fields)

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
    :has-options true}})

(def user-key-prefix "field-")

(defn user-key
  [sighting-field]
  (keyword (str user-key-prefix (if (map? sighting-field)
                                  (:sighting-field-key sighting-field)
                                  sighting-field))))
