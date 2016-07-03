(ns camelot.component.survey.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.component.survey.create :as create]
            [camelot.component.survey.manage :as manage]
            [om.dom :as dom]))

(defn survey-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(do
                                (nav/nav! (str "/" (:survey-id data)))
                                (nav/analytics-event "org-survey" "survey-click"))}
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
                        (om/build-all survey-list-component
                                      (sort-by :survey-id (:list data))
                                      {:key :survey-id}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do
                                            (nav/nav! "/survey/create")
                                            (nav/analytics-event "org-survey" "create-click"))}
                           (dom/span #js {:className "fa fa-plus"})
                           " Create Survey")
               (dom/button #js {:className "btn btn-default"
                                :onClick #(do
                                            (nav/nav! "/surveys")
                                            (nav/analytics-event "org-survey" "advanced-click"))}
                           "Advanced")))))

(defn survey-view-component
  "Render the view component for managing a survey."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :deployment-page-state {:menu [{:action :deployment
                                                      :name "Manage Deployments"
                                                      :active true}
                                                     {:action :upload
                                                      :name "Upload Captures"}]
                                              :active :deployment}))
    om/IRender
    (render [_]
      (when (:deployment-page-state app)
        (om/build manage/survey-management-component (:deployment-page-state app))))))
