(ns camelot.component.surveys
  (:require [camelot.util :as util]
            [camelot.nav :as nav]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]))

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
      (rest/get-surveys #(om/update! app :surveys %)))
    om/IRender
    (render [_]
      (if (empty? (:surveys app))
        (om/build survey-list-view-component (:surveys app))
        (om/build create-survey-view-component app)))))
