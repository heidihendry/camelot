(ns camelot.component.survey.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.component.survey.create :as create]
            [om.dom :as dom]))

(defn survey-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(nav/nav! (str "/" (:survey-id data) "/library"))}
               (dom/span #js {:className "menu-item-title"}
                         (:survey-name data))
               (dom/span #js {:className "menu-item-description"}
                         (:survey-notes data))))))

(defn create-view-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build create/create-survey-view-component app))))

(defn survey-menu-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all survey-list-component (:list data)
                                      {:key :survey-id}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(nav/nav! "/survey/create")}
                           (dom/span #js {:className "fa fa-plus"})
                           " Create Survey")))))


