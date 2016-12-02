(ns camelot.component.bulk-import.mapper
  "Bulk import data to field mapping components."
  (:require
   [cljs.core.async :refer [chan]]
   [om.core :as om]
   [camelot.translation.core :as tr]
   [camelot.util.model :as model]
   [camelot.component.upload :as upload]
   [om.dom :as dom]
   [camelot.nav :as nav]
   [camelot.state :as state]
   [cljs.core.async :refer [<! chan >!]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn upload-success-handler
  [data r]
  (om/update! data :column-properties (:response r)))

(defn field-mapping-option
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (first data)}
                  (first data)))))

(defn field-mapping-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [field (:field data)
            column-properties (:column-properties data)
            mappings (:mappings data)]
        (dom/div nil
                 (dom/label #js {:className "field-label"}
                            (name (first field)))
                 (dom/select #js {:className "field-input"
                                  :onChange #(go (>! (:chan state)
                                                     {:mapping (hash-map (first field)
                                                                         (.. % -target -value))}))}
                             (om/build-all field-mapping-option
                                           (sort-by first (conj column-properties
                                                                (hash-map "" {})))
                                           {:key first}))
                 (if-let [m (get mappings (first field))]
                   (dom/label nil
                              (model/reason-mapping-invalid (first field)
                                                            (get column-properties m)
                                                            tr/translate))))))))

(defn bulk-import-mapping-component
  [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:chan (chan)})
    om/IDidMount
    (did-mount [_]
      (go
        (loop []
          (let [r (<! (om/get-state owner :chan))]
            (cond
              (:mapping r)
              (let [v (apply vec (:mapping r))]
                (om/update! data [:mappings (first v)] (second v)))))
          (recur))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (tr/translate ::title)))
               (dom/div #js {:className "single-section"}
                        (om/build upload/file-upload-component data
                                  {:init-state {:chan (chan)}
                                   :opts {:analytics-event "mapping-upload"
                                          :success-handler (partial upload-success-handler data)
                                          :failure-handler #(prn "Fail")
                                          :endpoint "/surveys/bulkimport/columnmap"}})
                        (if-let [cps (:column-properties data)]
                          (let [colmaps (:mappings data)]
                            (dom/div nil
                                     (dom/h5 nil "Required fields")
                                     (om/build-all
                                      field-mapping-component
                                      (mapv #(hash-map :column-properties cps
                                                       :mappings colmaps
                                                       :field %
                                                       :vkey (first %))
                                            (sort-by first (-> model/schema-definitions
                                                               model/mappable-fields
                                                               model/required-fields)))
                                      {:init-state state
                                       :key :vkey})
                                     (dom/h5 nil "Optional fields")
                                     (om/build-all
                                      field-mapping-component
                                      (mapv #(hash-map :column-properties cps
                                                       :mappings colmaps
                                                       :field %
                                                       :vkey (first %))
                                            (sort-by first (-> model/schema-definitions
                                                               model/mappable-fields
                                                               model/optional-fields)))
                                      {:init-state state
                                       :key :vkey})
                                     (dom/button #js {:className "btn btn-primary pull-right"}
                                                 (tr/translate :words/submit))))))))))

(defn bulk-import-mapping-view
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :bulk-import {:mapping {}}))
    om/IRender
    (render [_]
      (if-let [data (:bulk-import app)]
        (om/build bulk-import-mapping-component data)))))
