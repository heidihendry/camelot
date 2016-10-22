(ns camelot.model.state
  (:require [schema.core :as s]))

(def Config
  {(s/required-key :erroneous-infrared-threshold) s/Num
   (s/required-key :infrared-iso-value-threshold) s/Int
   (s/required-key :language) (s/enum :en :vn)
   (s/required-key :root-path) (s/maybe s/Str)
   (s/required-key :night-end-hour) s/Int
   (s/required-key :night-start-hour) s/Int
   (s/required-key :project-start) org.joda.time.DateTime
   (s/required-key :project-end) org.joda.time.DateTime
   (s/required-key :submit-analytics) (s/maybe s/Bool)
   (s/required-key :sighting-independence-minutes-threshold) s/Num
   (s/required-key :surveyed-species) [s/Str]
   (s/required-key :required-fields) [[s/Keyword]]
   (s/optional-key :rename) s/Any
   (s/optional-key :timezone) s/Str})

(def State
  {(s/required-key :config) Config
   (s/optional-key :connection) clojure.lang.PersistentArrayMap})
