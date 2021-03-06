(ns camelot.component.survey.manage
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [camelot.util.capture :as capture]
            [camelot.component.species.core :as species]
            [camelot.component.deployment.recent :as recent]
            [camelot.component.survey.create :as create]
            [camelot.component.survey.file :as file]
            [camelot.component.survey.settings :as settings]
            [camelot.component.bulk-import.core :as bulk-import]
            [camelot.translation.core :as tr]
            [om.dom :as dom]
            [cljs.core.async :refer [<! chan >!]]
            [camelot.state :as state]
            [camelot.rest :as rest]
            [cljs-time.format :as tf]
            [camelot.component.deployment.core :as deployment])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn action-item-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className (str "menu-item"
                                    (if (= (:action data) (:active state)) " active" ""))
                    :onClick #(do
                                (go (>! (:active-chan state) (:action data)))
                                (nav/analytics-event "survey"
                                                     (str (name (:action data)) "-click")))}
               (dom/span #js {:className "menu-item-title"}
                         (:name data))))))

(defn action-menu-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:active-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [chan (om/get-state owner :active-chan)]
        (go
          (loop []
            (let [r (<! chan)]
              (om/update! data :active r)
              (recur))))))
    om/IRenderState
    (render-state [_ state]
      (when (:menu data)
        (dom/div #js {:className "section simple-menu"}
                 (om/build-all action-item-component
                               (filter #(if (nil? (:condition %))
                                          true
                                          (:condition %)) (:menu data))
                               {:key :action
                                :state (assoc state :active (:active data))}))))))

(defn survey-section-containers-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "section-container"}
                        (om/build action-menu-component data))
               (dom/div #js {:className "section-container"}
                        (case (:active data)
                          :deployment (om/build deployment/deployment-list-section-component data)
                          :upload (om/build recent/recent-deployment-section-component data)
                          :species (om/build species/species-menu-component (:species data))
                          :files (om/build file/file-menu-component data)
                          :import (om/build bulk-import/bulk-import-component data)
                          :settings (om/build settings/settings-menu-component data)
                          ""))))))

(defn survey-management-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "back-button-container"}
                        (dom/button #js {:className "btn btn-default back"
                                         :onClick #(nav/nav-up! 1)}
                                    (dom/span #js {:className "fa fa-mail-reply"})
                                    " " (tr/translate :words/back)))
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (get-in (state/app-state-cursor)
                                            [:selected-survey :survey-name :value])))
               (dom/div nil (om/build survey-section-containers-component data))))))
