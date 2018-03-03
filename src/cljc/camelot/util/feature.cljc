(ns camelot.util.feature)

(def default-feature-state
  "Map of feature keys and whether or not they're enabled by default."
  {:backup false})

(defn enabled?
  [config feature]
  (if (contains? (:features config) feature)
    (get-in config [:features feature])
    (or (get default-feature-state feature)
        false)))
