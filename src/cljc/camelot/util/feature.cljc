(ns camelot.util.feature
  (:require
   #?(:clj [schema.core :as s])))

(def default-feature-state
  "Map of feature keys and whether or not they're enabled by default."
  {:bulk-import true
   :sighting-tags false})

(defn enabled?
  [config feature]
  (if (contains? (:features config) feature)
    (get-in config [:features feature])
    (or (get default-feature-state feature)
        false)))
