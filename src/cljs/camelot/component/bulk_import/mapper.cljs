(ns camelot.component.bulk-import.mapper
  "Bulk import data to field mapping components."
  (:require
   [cljs.core.async :refer [chan]]
   [om.core :as om]
   [camelot.translation.core :as tr]
   [camelot.component.util :as util]
   [camelot.util.model :as model]
   [camelot.component.upload :as upload]
   [om.dom :as dom]
   [camelot.nav :as nav]
   [camelot.state :as state]
   [cljs.core.async :refer [<! chan >!]]
   [camelot.rest :as rest])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn get-import-data
  [data]
  {:file-data (deref (:file-data data))
   :mappings (deref (:mappings data))
   :survey-id (state/get-survey-id)})

(defn submit-mappings
  [data]
  (om/update! data :show-import-status-dialog true)
  (om/update! data :import-status :initialising)
  (rest/post-x "/surveys/bulkimport/import" {:data (get-import-data data)}
               #(om/update! data :import-status :active)
               #(om/update! data :import-status :failed)))

(defn upload-success-handler
  [data r]
  (om/update! data :upload-pending false)
  (om/update! data :column-properties (get-in r [:response :column-properties]))
  (om/update! data :file-data (get-in r [:response :file-data]))
  (if (empty? (:mappings data))
    (om/update! data :mappings (get-in r [:response :default-mappings]))))

(defn upload-pending-handler
  [data r]
  (om/update! data :upload-pending true))

(defn field-mapping-option
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (first data)}
                  (first data)))))

(defn validation-summary
  "Return a summary of the validation state"
  [problem]
  (case problem
    :mismatch {:result :fail :reason (tr/translate ::validation-mismatch)}
    :missing {:result :fail :reason (tr/translate ::validation-missing)}
    {:result :pass :reason (tr/translate ::validation-passed)}))

(defn cancel-button-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when-not (:upload-pending data)
        (dom/button #js {:className "btn btn-default"
                         :onClick #(om/transact! data :page dec)}
                    )
        (dom/button #js {:className "btn btn-default"
                         :onClick #(nav/nav-up! 2)}
                    (if (:column-properties data)
                      (tr/translate :words/cancel)
                      (dom/span nil
                                (dom/span #js {:className "fa fa-chevron-left"})
                                " "
                                (tr/translate :words/back))))))))

(defn field-mapping-component
  [data owner {:keys [required]}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [field (:field data)
            column-properties (:column-properties data)
            mappings (:mappings data)]
        (dom/div nil
                 (dom/label #js {:className (str "field-label" (if required " required" ""))}
                            (tr/translate (str "report/" (name (first field)))))
                 (dom/select #js {:className "field-input"
                                  :onChange #(go (>! (:chan state)
                                                     {:mapping (hash-map (first field)
                                                                         (let [v (.. % -target -value)]
                                                                           (if (or (nil? v) (empty? v))
                                                                             nil
                                                                             v)))}))
                                  :value (get mappings (first field))}
                             (om/build-all field-mapping-option
                                           (sort-by first (conj column-properties
                                                                   (hash-map "" {})))
                                           {:key first}))
                 (if-let [m (get mappings (first field))]
                   (dom/label #js {:className "validation-warning"}
                              (model/reason-mapping-invalid
                               model/extended-schema-definitions
                               (first field)
                               (get column-properties m)
                               tr/translate))))))))

(defn import-status-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (case (:import-status data)
        :complete (dom/p nil "Import complete (enable close button)")
        :active (dom/p nil "Import active")
        :failed (dom/p nil "Import failed")
        :initialising (dom/p nil "Import initialising")
        nil (dom/p nil "Unsure what's going on")))))

(defn import-status-dialog
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build util/prompt-component data
                {:opts {:active-key :show-import-status-dialog
                        :title (tr/translate ::import-status-dialog-title)
                        :body (dom/div nil (om/build import-status-component data))
                        :actions (dom/div #js {:className "button-container"}
                                          (dom/button #js {:className "btn btn-default"
                                                           :ref "action-first"
                                                           :onClick #(do
                                                                       (om/update! data :show-import-status-dialog false)
                                                                       (om/update! data :import-status nil))}
                                                      (tr/translate :words/cancel)))}}))))

(defn compare-column-schema-weight
  "Sort based on column schema weightings."
  [a b]
  (let [da (get model/schema-definitions (first a))
        db (get model/schema-definitions (first b))]
    (let [o (compare (:order da) (:order db))]
      (if (zero? o)
        (compare (first a) (first b))
        o))))

(defn column-mapping-form-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
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
                          (sort compare-column-schema-weight
                                (-> model/schema-definitions
                                    model/mappable-fields
                                    model/required-fields
                                    model/with-absolute-path)))
                    {:init-state state
                     :opts {:required true}
                     :key :vkey})
                   (dom/h5 nil "Optional fields")
                   (om/build-all
                    field-mapping-component
                    (mapv #(hash-map :column-properties cps
                                     :mappings colmaps
                                     :field %
                                     :vkey (first %))
                          (sort compare-column-schema-weight
                                (-> model/schema-definitions
                                    model/mappable-fields
                                    model/optional-fields)))
                    {:init-state state
                     :key :vkey})
                   (let [vs (validation-summary (:validation-problem data))]
                     (dom/div nil
                              (when (= (:result vs) :fail)
                                (dom/label #js {:className "validation-warning"}
                                           (:reason vs)))
                              (dom/div #js {:className "button-container pull-right"}
                                       (om/build cancel-button-component data)
                                       (dom/button #js {:className "btn btn-primary"
                                        ;:disabled (if (:validation-problem data) "disabled" nil)
                                                        :onClick #(submit-mappings data)
                                                        :title (:reason vs)}
                                                   (tr/translate :words/submit)))))))
        (dom/div #js {:className "button-container pull-right"}
                 (om/build cancel-button-component data))))))

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
    om/IWillUpdate
    (will-update [_ _ _]
      (let [fs (-> model/schema-definitions
                   model/mappable-fields
                   model/with-absolute-path)]
        (om/update! data :validation-problem
                    (or
                     (reduce #(let [m (get data [:mappings (first %2)])]
                                (if (and m
                                         (model/reason-mapping-invalid
                                          model/extended-schema-definitions
                                          (first %2)
                                          (get-in data [:column-properties m])
                                          identity))
                                  (reduced :mismatch)))
                             nil fs)
                     (reduce #(let [m (get data [:mappings (first %2)])]
                                (if (nil? m)
                                  (reduced :missing)))
                             nil (model/required-fields fs))))))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "split-menu"}
               (om/build import-status-dialog data)
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (tr/translate ::title)))
               (dom/div #js {:className "single-section"}
                        (om/build upload/file-upload-component data
                                  {:init-state {:chan (chan)}
                                   :opts {:analytics-event "mapping-upload"
                                          :pending-handler (partial upload-pending-handler data)
                                          :success-handler (partial upload-success-handler data)
                                          :failure-handler #(om/update! data :upload-pending false)
                                          :endpoint "/surveys/bulkimport/columnmap"}})
                        (if (:upload-pending data)
                          (dom/div #js {:className "align-center"}
                                   (dom/img #js {:className "spinner"
                                                 :src "images/spinner.gif"
                                                 :height "32"
                                                 :width "32"})
                                   (dom/p nil
                                          (tr/translate ::scanning)))
                          (om/build column-mapping-form-component data
                                    {:init-state state})))))))

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
