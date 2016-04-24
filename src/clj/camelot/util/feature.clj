(ns camelot.util.feature
  (:require [environ.core :refer [env]]))

(def features
  {:survey false})

(defn enabled?
  [feature]
  (or (env :camelot-dev-mode)
      (env (keyword (str "camelot-feature-" (name feature))))
      (get features feature)))
