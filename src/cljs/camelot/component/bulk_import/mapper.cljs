(ns camelot.component.bulk-import.mapper
  "Bulk import data to field mapping components."
  (:require
   [cljs.core.async :refer [chan]]
   [om.core :as om]
   [camelot.util.data :as util.data]
   [camelot.translation.core :as tr]
   [camelot.component.util :as util]
   [camelot.util.model :as model]
   [camelot.component.upload :as upload]
   [om.dom :as dom]
   [camelot.nav :as nav]
   [camelot.state :as state]
   [cljs.core.async :refer [<! chan >!]]
   [camelot.rest :as rest]
   [clojure.string :as str])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(def bulk-import-ui-sample-image "images/bulk-import-sample.png")

(defn get-import-data
  [data]
  {:file-data (deref (:file-data data))
   :mappings (deref (:mappings data))
   :survey-id (state/get-survey-id)})

(defn submit-mappings
  [data]
  (om/update! data :show-import-status-dialog true)
  (om/update! data :import-status :initialising)
  (rest/post-x "/import/bulk/import" {:data (get-import-data data)}
               #(if (or (nil? (:body %)) (empty? (:body %)))
                  (do
                    (om/update! (state/bulk-import-state) :polling-active true)
                    (om/update! data :import-status :active))
                  (do
                    (om/update! data :import-status :validation-problem)
                    (om/update! data :import-status-details (:body %))))
               #(do
                  (om/update! data :import-status :failed)
                  (om/update! data :import-status-details {:errors (:body %)
                                                           :status (:status %)}))))

(defn upload-success-handler
  [data r]
  (om/update! data :upload-pending false)
  (om/update! data :column-properties (get-in r [:response :column-properties]))
  (om/update! data :file-data (get-in r [:response :file-data]))
  (if (or (nil? (:mappings data)) (empty? (deref (:mappings data))))
    (om/update! data :mappings (get-in r [:response :default-mappings]))))

(defn upload-pending-handler
  [data r]
  (om/update! data :upload-pending true)
  (om/update! data :upload-failed nil))

(defn field-mapping-option
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (first data)}
                  (first data)))))

(def ^:private sighting-fields [:taxonomy-genus
                                :taxonomy-species
                                :sighting-quantity
                                :taxonomy-common-name])

(defn- sightings-partially-mapped?
  [mappings]
  (let [mapped-count (->> (select-keys mappings sighting-fields)
                          (map (fn [[k v]] v))
                          (remove nil?)
                          count)]
    (not (or (zero? mapped-count) (= mapped-count (count sighting-fields))))))

(defn translated-sighting-field-names
  "Pretty human-readable list of sighting fields."
  []
  (tr/list-to-user-string
   (map #(tr/translate (keyword (str "report/" (name %)))) sighting-fields)))

(defn validation-summary
  "Return a summary of the validation state"
  [problem mappings]
  (cond
    (= problem :mismatch)
    {:result :fail :reason (tr/translate ::validation-mismatch)}

    (= problem :missing)
    {:result :fail :reason (tr/translate ::validation-missing)}

    (sightings-partially-mapped? mappings)
    {:result :fail :reason (tr/translate ::sightings-partially-mapped
                                         (translated-sighting-field-names))}

    :default
    {:result :pass :reason (tr/translate ::validation-passed)}))

(defn cancel-button-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when-not (:upload-pending data)
        (dom/button #js {:className "btn btn-default"
                         :onClick #(nav/nav-up! 2)}
                    (if (:column-properties data)
                      (tr/translate :words/cancel)
                      (dom/span nil
                                (dom/span #js {:className "fa fa-mail-reply"})
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
                            (or (:label (second field))
                                (tr/translate (str "report/" (name (first field))))))
                 (dom/select #js {:className "field-input"
                                  :onChange #(do
                                               (.persist %)
                                               (go (>! (:chan state)
                                                       {:mapping (hash-map (first field)
                                                                           (let [v (.. % -target -value)]
                                                                             (if (or (nil? v) (empty? v))
                                                                               nil
                                                                               v)))})))
                                  :value (get mappings (first field))}
                             (om/build-all field-mapping-option
                                           (sort-by first (conj column-properties
                                                                (hash-map "" {})))
                                           {:key first}))
                 (if-let [m (get mappings (first field))]
                   (dom/label #js {:className "validation-warning"}
                              (model/reason-mapping-invalid
                               (:mappable-fields data)
                               (first field)
                               (get column-properties m)
                               tr/translate))))))))

(defn error-item-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil (:error data)))))

(defn import-status-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (when (:import-status data)
        (case (:import-status data)
          :active
          (dom/div nil
                   (dom/p nil (tr/translate ::import-started))
                   (dom/div #js {:className "bulk-import-sample-image"}
                            (dom/img #js {:src bulk-import-ui-sample-image
                                          :alt (tr/translate ::sample-ui)})))

          :validation-problem
          (dom/div nil
                   (dom/p nil
                          (tr/translate ::validation-problem))
                   (dom/div #js {:className "bulk-import-validation-problem-list"}
                            (dom/textarea #js {:rows "6"
                                               :cols "42"
                                               :className "field-input"}
                                          (str/join "\n" (:import-status-details data)))))

          :failed
          (dom/div nil
                   (dom/p nil (tr/translate ::import-failed))
                   (dom/p nil
                          (dom/label nil (tr/translate ::status-code))
                          ": "
                          (get-in data [:import-status-details :status]))
                   (dom/textarea #js {:rows "6"
                                      :cols "42"
                                      :className "field-input"}
                                 (get-in data [:import-status-details :errors])))

          :initialising
          (dom/p nil (tr/translate ::initialising)))))))

(defn import-status-dialog
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build util/prompt-component data
                {:opts {:active-key :show-import-status-dialog
                        :title (tr/translate ::import-status-dialog-title)
                        :body (dom/div nil (om/build import-status-component data))
                        :closable false
                        :actions (dom/div #js {:className "button-container"}
                                          (dom/button #js {:className "btn btn-primary"
                                                           :ref "action-first"
                                                           :disabled (when (= (:import-status data) :initialising)
                                                                       "disabled")
                                                           :title (when (= (:import-status data) :initialising)
                                                                    (tr/translate ::please-wait))
                                                           :onClick #(do
                                                                       (om/update! data :show-import-status-dialog false)
                                                                       (om/update! data :import-status nil))}
                                                      (tr/translate :words/continue)))}}))))

(defn compare-column-schema-weight
  "Sort based on column schema weightings."
  [schema-definitions a b]
  (let [da (get schema-definitions (first a))
        db (get schema-definitions (first b))]
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
        (let [colmaps (:mappings data)
              mappable (into {} (:mappable-fields data))]
          (dom/div nil
                   (dom/h5 nil "Required fields")
                   (om/build-all
                    field-mapping-component
                    (mapv #(hash-map :column-properties cps
                                     :mappings colmaps
                                     :mappable-fields (into {} (:mappable-fields data))
                                     :field %
                                     :vkey (first %))
                          (sort (partial compare-column-schema-weight mappable)
                                (model/required-fields (:mappable-fields data))))
                    {:init-state state
                     :opts {:required true}
                     :key :vkey})
                   (dom/h5 nil "Optional fields")
                   (om/build-all
                    field-mapping-component
                    (mapv #(hash-map :column-properties cps
                                     :mappings colmaps
                                     :mappable-fields mappable
                                     :field %
                                     :vkey (first %))
                          (sort (partial compare-column-schema-weight mappable)
                                (model/optional-fields (:mappable-fields data))))
                    {:init-state state
                     :key :vkey})
                   (let [vs (validation-summary (:validation-problem data) colmaps)]
                     (dom/div nil
                              (when (= (:result vs) :fail)
                                (dom/label #js {:className "validation-warning"}
                                           (:reason vs)))
                              (dom/div #js {:className "button-container pull-right"}
                                       (om/build cancel-button-component data)
                                       (dom/button #js {:className "btn btn-primary"
                                                        :disabled (when (or (= (:result vs) :fail)
                                                                            (:show-import-status-dialog data))
                                                                    "disabled")
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
    om/IWillMount
    (will-mount [_]
      (let [fields (-> model/schema-definitions
                       model/mappable-fields
                       model/with-absolute-path)]
        (letfn [(survey-fields [all-sf]
                  (let [survey-sf (filter #(= (state/get-survey-id) (:survey-id %)) all-sf)]
                    (om/update! data :mappable-fields
                                (model/with-sighting-fields fields survey-sf))))]
          (rest/get-x-opts "/sighting-fields"
                           {:success #(survey-fields (:body %))
                            :failure #(om/update! data :mappable-fields
                                                  (model/with-sighting-fields fields fields))}))))
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
      (let [fs (:mappable-fields @data)]
        (om/update! data :validation-problem
                    (or
                     (reduce #(let [m (get-in @data [:mappings (first %2)])]
                                (if (and m
                                         (model/reason-mapping-invalid
                                          (into {} fs)
                                          (first %2)
                                          (get-in @data [:column-properties m])
                                          identity))
                                  (reduced :mismatch)))
                             nil fs)
                     (reduce #(let [m (get-in @data [:mappings (first %2)])]
                                (if (nil? m)
                                  (reduced :missing)))
                             nil (model/required-fields fs))))))
    om/IRenderState
    (render-state [_ state]
      (if (:mappable-fields data)
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
                                            :failure-handler #(do (om/update! data :upload-pending false)
                                                                  (om/update! data :upload-failed true))
                                            :endpoint "/import/bulk/columnmap"}})
                          (cond
                            (:upload-pending data)
                            (dom/div #js {:className "align-center"}
                                     (dom/img #js {:className "spinner"
                                                   :src "images/spinner.gif"
                                                   :height "32"
                                                   :width "32"})
                                     (dom/p nil
                                            (tr/translate ::scanning)))

                            (:upload-failed data)
                            (dom/p #js {:className "validation-warning"
                                        :style #js {:marginTop "1rem"}}
                                   (dom/label nil (tr/translate ::invalid-csv)))

                            :default
                            (om/build column-mapping-form-component data
                                      {:init-state state}))))
        (dom/div #js {:className "align-center"}
                                     (dom/img #js {:className "spinner"
                                                   :src "images/spinner.gif"
                                                   :height "32"
                                                   :width "32"}))))))

(defn bulk-import-mapping-view
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :bulk-import {:mappings {}}))
    om/IWillUnmount
    (will-unmount [_]
      (om/update! app :bulk-import nil))
    om/IRender
    (render [_]
      (if-let [data (:bulk-import app)]
        (om/build bulk-import-mapping-component data)))))
