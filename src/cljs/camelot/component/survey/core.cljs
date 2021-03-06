(ns camelot.component.survey.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [camelot.component.survey.create :as create]
            [camelot.component.survey.manage :as manage]
            [camelot.component.survey.settings :as settings]
            [camelot.component.survey.sighting-fields :as sighting-fields]
            [om.dom :as dom]
            [camelot.translation.core :as tr]
            [camelot.rest :as rest]
            [cljs.core.async :refer [<! chan >!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn bulk-import-available?
  []
  (get-in (state/app-state-cursor)
          [:selected-survey :survey-bulk-import-available :value]))

(def survey-menu
  [{:action :deployment
    :name (tr/translate ::manage-traps)}
   {:action :upload
    :name (tr/translate ::upload-captures)
    :condition (complement bulk-import-available?)}
   {:action :import
    :name (tr/translate ::import)
    :condition bulk-import-available?}
   {:action :species
    :name (tr/translate ::species)}
   {:action :files
    :name (tr/translate ::files)}
   {:action :settings
    :name (tr/translate ::settings)}])

(defn survey-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(do
                                (nav/nav! (str "/" (:survey-id data)))
                                (nav/analytics-event "org-survey" "survey-click"))}
               (dom/span #js {:className "menu-item-title"}
                         (:survey-name data))
               (dom/span #js {:className "menu-item-description"}
                         (:survey-notes data))))))

(defn edit-details-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build settings/edit-details-component app))))

(defn sighting-fields-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build sighting-fields/manage-fields-component app))))

(defn create-view-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build create/create-survey-view-component app))))

(defn survey-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all survey-list-component
                                      (sort-by :survey-name (:list data))
                                      {:key :survey-id
                                       :init-state state}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do
                                            (nav/nav! "/survey/create")
                                            (nav/analytics-event "org-survey" "create-click"))}
                           (dom/span #js {:className "fa fa-plus"})
                           " " (tr/translate ::create-survey))
               (dom/button #js {:className "btn btn-default"
                                :onClick #(do
                                            (nav/nav! "/surveys")
                                            (nav/analytics-event "org-survey" "advanced-click"))}
                           (tr/translate :words/advanced))))))

(defn survey-view-component
  "Render the view component for managing a survey."
  [app owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (when-not (:survey-page-state @app)
        (om/update! app :survey-page-state
                    {:species {}}))

      (om/update! app [:survey-page-state :menu]
                  (vec (filter #(if (nil? (:condition %))
                                  true
                                  ((:condition %)))
                               survey-menu)))

      (let [active (get-in @app [:survey-page-state :active])]
        (when-not (and active
                       (contains? (set (map :action (:menu (:survey-page-state @app))))
                                  active))
          (om/transact! app [:survey-page-state]
                        (fn [d]
                          (assoc d :active (->> (:menu d)
                                                first
                                                :action)))))))
    om/IRender
    (render [_]
      (if (seq (:survey-page-state app))
        (om/build manage/survey-management-component (:survey-page-state app))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
