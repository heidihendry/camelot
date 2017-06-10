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
    :translation-key :datatype/number}})
