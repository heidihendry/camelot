(ns camelot.component.survey.bulk-import
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

(defn upload-success-handler
  [data r]
  (om/update! data :column-properties (:response r)))

(defn bulk-import-component
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
                 (dom/div #js {:className "help-text"}
                          (tr/translate ::help-text-step-1))
                 (dom/a #js {:href "/surveys/bulkimport/template"}
                        (dom/button #js {:className "btn btn-primary full-width"}
                                    (tr/translate ::download)))
                 (dom/div #js {:className "sep"})
                 (dom/div #js {:className "help-text"}
                          (tr/translate ::help-text-step-2))
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(nav/nav! (str "/" (state/get-survey-id) "/bulk-import"))}
                             (tr/translate ::ready-to-upload)))))))

(defn field-mapping-option
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (first data)}
                  (first data)))))

(defn field-mapping-component
  [data owner {:keys [column-properties]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/label #js {:className "field-label"}
                          (name (first data)))
               (dom/select #js {:className "field-input"
                                :onChange #(go (>! (:chan state)
                                                   {:mapping (hash-map (first data)
                                                                       (.. % -target -value))}))}
                           (om/build-all field-mapping-option
                                         (conj (hash-map "" {})
                                               column-properties)))))))

(defn bulk-import-mapping-view
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IWillMount
    (will-mount [_]
      (om/update! app :bulk-import {:mapping {}})
      (go
        (loop []
          (let [r (<! (om/get-state owner :chan))]
            (cond
              (:mapping r)
              (om/update! (-> app :bulk-import :mapping)
                          (first (:mapping r))
                          (second (:mapping r)))))
          (recur))))
    om/IRenderState
    (render-state [_ state]
      (when (:bulk-import app)
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "intro"}
                          (dom/h4 nil (tr/translate ::title)))
                 (dom/div #js {:className "single-section"}
                          (om/build upload/file-upload-component app
                                    {:init-state {:chan (chan)}
                                     :opts {:analytics-event "mapping-upload"
                                            :success-handler (partial upload-success-handler (:bulk-import app))
                                            :failure-handler #(prn "Fail")
                                            :endpoint "/surveys/bulkimport/columnmap"}})
                          (if-let [cps (get-in app [:bulk-import :column-properties])]
                            (dom/div nil
                                     (dom/h5 nil "Required fields")
                                     (om/build-all
                                      field-mapping-component
                                      (sort-by first (-> model/schema-definitions
                                                         model/mappable-fields
                                                         model/required-fields))
                                      {:opts {:column-properties cps}
                                       :init-state state
                                       :key first})
                                     (dom/h5 nil "Optional fields")
                                     (om/build-all
                                      field-mapping-component
                                      (sort-by first (-> model/schema-definitions
                                                         model/mappable-fields
                                                         model/optional-fields))
                                      {:opts {:column-properties cps}
                                       :init-state state
                                       :key first})
                                     (dom/button #js {:className "btn btn-primary pull-right"}
                                                 (tr/translate :words/submit))))))))))
