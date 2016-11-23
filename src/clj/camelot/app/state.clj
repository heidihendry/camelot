(ns camelot.app.state
  "Application state."
  (:require
   [schema.core :as s]))

(def Config
  {(s/required-key :erroneous-infrared-threshold) s/Num
   (s/required-key :infrared-iso-value-threshold) s/Int
   (s/required-key :language) (s/enum :en :vn)
   (s/required-key :root-path) (s/maybe s/Str)
   (s/required-key :night-end-hour) s/Int
   (s/required-key :night-start-hour) s/Int
   (s/required-key :project-start) org.joda.time.DateTime
   (s/required-key :project-end) org.joda.time.DateTime
   (s/required-key :send-usage-data) s/Bool
   (s/required-key :sighting-independence-minutes-threshold) s/Num
   (s/required-key :surveyed-species) [s/Str]
   (s/required-key :required-fields) [[s/Keyword]]
   (s/optional-key :rename) s/Any
   (s/optional-key :timezone) s/Str
   (s/optional-key :features) (s/maybe {s/Keyword s/Bool})})

(def State
  {(s/required-key :config) Config
   (s/optional-key :connection) clojure.lang.PersistentArrayMap
   (s/optional-key :camera-status-active-id) s/Int})

(defn gen-state
  "Return the global application state.
Currently the only application state is the user's configuration."
  [conf]
  {:config conf})
