(ns camelot.component.create-survey
  (:require [om.core :as om]
            [camelot.component.species-search :as search]))

(defn create-survey-view-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build search/species-search-component data))))
