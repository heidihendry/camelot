(ns camelot.component.survey.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.util.feature :as feature]
            [camelot.state :as state]
            [camelot.component.survey.create :as create]
            [camelot.component.survey.manage :as manage]
            [om.dom :as dom]
            [camelot.translation.core :as tr]
            [camelot.rest :as rest]
            [cljs.core.async :refer [<! chan >!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn delete
  "Delete the survey and trigger a removal event."
  [state data event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (js/confirm (tr/translate ::confirm-delete))
    (rest/delete-x (str "/surveys/" (:survey-id data))
                   #(go (>! (:chan state) {:event :delete
                                           :data data})))))

(defn bulk-import-mode?
  []
  (and (feature/enabled? (state/settings) :bulk-import)
       (get-in (state/app-state-cursor)
               [:selected-survey :survey-bulk-import-mode :value])))

(defn survey-list-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "menu-item detailed dynamic"
                    :onClick #(do
                                (nav/nav! (str "/" (:survey-id data)))
                                (nav/analytics-event "org-survey" "survey-click"))}
               (dom/div #js {:className "pull-right fa fa-times remove top-corner"
                             :onClick (partial delete state data)})
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
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IDidMount
    (did-mount [_]
      (let [ch (om/get-state owner :chan)]
        (go
          (loop []
            (let [r (<! ch)]
              (cond
                (= (:event r) :delete)
                (om/transact! data :list #(remove (fn [x] (= x (:data r))) %))))
            (recur)))))
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
    om/IWillUnmount
    (will-unmount [_]
      (om/update! app :survey-page-state {}))
    om/IDidMount
    (did-mount [_]
      (om/update! app :survey-page-state
                  {:menu [{:action :deployment
                           :name (tr/translate ::manage-traps)
                           :condition (not (bulk-import-mode?))}
                          {:action :upload
                           :name (tr/translate ::upload-captures)
                           :condition (not (bulk-import-mode?))}
                          {:action :import
                           :name (tr/translate ::import)
                           :condition (bulk-import-mode?)}
                          {:action :species
                           :name (tr/translate ::species)}
                          {:action :files
                           :name (tr/translate ::files)}]
                   :active :deployment
                   :species {}})
      (om/transact! app [:survey-page-state :menu]
                    (fn [d]
                      (let [m (first (filter #(if (nil? (:condition %))
                                                true
                                                (:condition %)) d))]
                        (vec (conj (remove #{m} d)
                                   (assoc m :active true))))))
      (om/transact! app [:survey-page-state]
                    (fn [d]
                      (assoc d :active (->> (:menu d)
                                            (filter #(:active %))
                                            first
                                            :action)))))
    om/IRender
    (render [_]
      (if (seq (:survey-page-state app))
        (om/build manage/survey-management-component (:survey-page-state app))
        (dom/div #js {:className "align-center"}
                 (dom/img #js {:className "spinner"
                               :src "images/spinner.gif"
                               :height "32"
                               :width "32"}))))))
