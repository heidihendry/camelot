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

(defn get-survey-details
  []
  (->> (get-in (state/app-state-cursor) [:survey :list])
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
                               :onChange #(om/update! data ::survey-directory (.. % -target -value))
                               :value (::survey-directory data)})
               (dom/a #js {:href (str "/surveys/bulkimport/template?dir=" (::survey-directory data))}
                      (dom/button #js {:className "btn btn-primary full-width"}
                                  (tr/translate ::download)))
               (dom/div #js {:className "sep"})
               (dom/div #js {:className "help-text"}
                        (tr/translate ::help-text-step-2))
               (dom/button #js {:className "btn btn-primary full-width"
                                :onClick #(nav/nav! (str "/" (state/get-survey-id) "/bulk-import"))}
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
