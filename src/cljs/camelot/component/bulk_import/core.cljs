(ns camelot.component.bulk-import.core
  (:require [cljs.core.async :refer [chan]]
            [om.core :as om]
            [camelot.translation.core :as tr]
            [camelot.util.model :as model]
            [camelot.component.upload :as upload]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [cljs.core.async :refer [<! chan >!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-survey-details
  []
  (->> (get-in (state/app-state-cursor) [:organisation :survey :list])
       (filter #(= (get % :survey-id) (state/get-survey-id)))
       first))

(defn bulk-import-content-component
  "Top-level template and import UI for bulk import."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "help-text"}
                        (tr/translate ::help-text-step-1))
               (dom/label #js {:className "field-label"}
                          (tr/translate ::survey-directory))
               (dom/input #js {:className "field-input"
                               :placeholder (tr/translate ::path-name-placeholder)
                               :onChange #(om/update! data ::survey-directory (.. % -target -value))
                               :value (::survey-directory data)})
               (dom/a #js {:href (str "/surveys/bulkimport/template?dir=" (::survey-directory data))}
                      (dom/button #js {:className "btn btn-primary full-width"
                                       :disabled (if (seq (::survey-directory data)) "" "disabled")}
                                  (tr/translate ::download)))
               (dom/div #js {:className "sep"})
               (dom/div #js {:className "help-text"}
                        (tr/translate ::help-text-step-2))
               (dom/button #js {:className "btn btn-primary full-width"
                                :onClick #(nav/nav! (str "/" (state/get-survey-id) "/bulk-import/mapper"))}
                           (tr/translate ::ready-to-upload))))))

(defn bulk-import-view
  "Top level bulk import rendered as a view."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! (state/app-state-cursor) :bulk-import {}))
    om/IRenderState
    (render-state [_ state]
      (when (:bulk-import (state/app-state-cursor))
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "back-button-container"}
                          (dom/button #js {:className "btn btn-default back"
                                           :onClick #(nav/nav-up! 1)}
                                      (dom/span #js {:className "fa fa-mail-reply"})
                                      " " (tr/translate ::survey-menu)))
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::title))
                          (dom/h5 nil (:survey-name (get-survey-details))))
                 (dom/div #js {:className "single-section"}
                          (om/build bulk-import-content-component data)))))))

(defn bulk-import-component
  "Top level bulk import rendered as a panel component."
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! (state/app-state-cursor) :bulk-import {}))
    om/IRenderState
    (render-state [_ state]
      (when (:bulk-import (state/app-state-cursor))
        (dom/div #js {:className "section"}
                 (om/build bulk-import-content-component data))))))
