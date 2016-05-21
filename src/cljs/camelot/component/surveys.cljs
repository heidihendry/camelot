(ns camelot.component.surveys
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn survey-list-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/span nil "List stuff will go here"))))

(defn create-survey-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/span nil "Creation stuff will go here"))))

(defn surveys-view-component [app owner]
  "Render an album validation summary."
  (reify
    om/IWillMount
    (om/will-mount [_]
                                        ;(rest/get-surveys #(om/update! app :surveys %))
      )
    om/IRender
    (render [_]
      (if (empty? (:surveys app))
        (om/build survey-list-view-component (:surveys app))
        (om/build create-survey-view-component app)))))
