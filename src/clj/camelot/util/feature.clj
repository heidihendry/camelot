(ns camelot.util.feature
  (:require [environ.core :refer [env]]
            [schema.core :as s]))

(def features
  "Map of feature keys and whether or not they're enabled"
  {})

(s/defn enabled? :- s/Bool
  [feature :- s/Keyword]
  (or (env :camelot-dev-mode)
      (env (keyword (str "camelot-feature-" (name feature))))
      (or (get features feature) false)))
