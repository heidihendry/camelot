(ns camelot.component.library.identify
  (:require
   [om.dom :as dom]
   [om.core :as om]
   [camelot.translation.core :as tr]
   [camelot.component.library.util :as util]
   [camelot.rest :as rest]
   [camelot.nav :as nav]
   [clojure.string :as str]
   [camelot.util.cursorise :as cursorise]
   [camelot.util.feature :as feature]
   [camelot.state :as state]))

(defn show-panel?
  [data]
  (or (not= (get-in data [:search :mode]) :search)
      (get data :show-identification-panel)))

(defn species-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:taxonomy-id data)}
                  (:taxonomy-label data)))))

(defn add-sighting
  []
  (let [spp (cljs.reader/read-string (get-in (state/library-state) [:identification :species]))
        qty (get-in (state/library-state) [:identification :quantity])
        lifestage (get-in (state/library-state) [:identification :lifestage])
        sex (get-in (state/library-state) [:identification :sex])
        selected (:selected-media-id (state/library-state))
        all-selected (util/all-media-selected)]
    (rest/put-x "/library/identify" {:data (merge {:identification
                                                   {:quantity qty
                                                    :lifestage (if (util/unidentified? lifestage) nil lifestage)
                                                    :sex (if (util/unidentified? sex) nil sex)
                                                    :species spp}}
                                                  {:media
                                                   (mapv :media-id all-selected)})}
                (fn [resp]
                  (dorun (map #(do (om/update! (second %)
                                               :sightings
                                               (conj (:sightings (second %))
                                                     {:taxonomy-id spp
                                                      :sighting-lifestage lifestage
                                                      :sighting-sex sex
                                                      :sighting-id (first %)
                                                      :sighting-quantity qty}))
                                   (om/update! (second %) :media-processed true))
                              (zipmap (:body resp) all-selected)))
                  (util/show-identified-message)
                  (.focus (.getElementById js/document "media-collection-container"))
                  (om/update! (:identification (state/library-state)) :quantity 1)
                  (om/update! (:identification (state/library-state)) :species -1)
                  (om/update! (:identification (state/library-state)) :sex "unidentified")
                  (om/update! (:identification (state/library-state)) :lifestage "unidentified")))))

(defn sighting-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:key data)} (:label data)))))

(defn sighting-lifestage-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "field-input auto-input"
                       :value (get-in data [:identification :lifestage])
                       :onChange #(let [v (.. % -target -value)]
                                    (om/update! (:identification data) :lifestage v)
                                    (om/update! (:identification data) :dirty-state true))}
                  (om/build-all sighting-option-component
                                (list {:key "unidentified"
                                       :label (tr/translate :sighting/sighting-property.unidentified)}
                                      {:key "adult"
                                       :label (tr/translate :sighting/sighting-lifestage.adult)}
                                      {:key "juvenile"
                                       :label (tr/translate :sighting/sighting-lifestage.juvenile)})
                                {:key :key})))))

(defn sighting-sex-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "field-input auto-input"
                       :value (get-in data [:identification :sex])
                       :onChange #(let [v (.. % -target -value)]
                                    (om/update! (:identification data) :sex v)
                                    (om/update! (:identification data) :dirty-state true))}
                  (om/build-all sighting-option-component
                                (list {:key "unidentified"
                                       :label (tr/translate :sighting/sighting-property.unidentified)}
                                      {:key "M"
                                       :label (tr/translate :sighting/sighting-sex.male)}
                                      {:key "F"
                                       :label (tr/translate :sighting/sighting-sex.female)})
                                {:key :key})))))

(defn submit-identification
  []
  (add-sighting))

(defn validate-proposed-species
  [data]
  (and (not (nil? (:new-species-name data)))
       (= (count (str/split (:new-species-name data) #" ")) 2)))

(defn add-taxonomy-success-handler
  [data resp]
  (let [species (cursorise/decursorise (:body resp))]
    (om/transact! data :species #(conj % (hash-map (:taxonomy-id species) species)))
    (om/update! data :new-species-name nil)
    (om/update! (get-in data [:identification]) :species (str (:taxonomy-id species)))
    (om/update! data :taxonomy-create-mode false)))

(defn add-taxonomy-handler
  [data]
  (let [segments (str/split (:new-species-name data) #" ")]
    (rest/post-x "/taxonomy"
                 {:data (merge {:taxonomy-genus (first segments)
                                :taxonomy-species (second segments)
                                :taxonomy-common-name (str (first segments) " " (second segments))}
                               (if (and (:survey-id data) (not= (:survey-id data) -1))
                                 {:survey-id (:survey-id data)}
                                 {}))}
                 (partial add-taxonomy-success-handler data)))
  (nav/analytics-event "library-id" "taxonomy-create"))

(defn add-taxonomy-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-species data)]
        (dom/form #js {:className "field-input-form inline"
                       :onSubmit #(.preventDefault %)}
                  (dom/input #js {:className "field-input inline long-input"
                                  :autoFocus "autofocus"
                                  :placeholder (tr/translate ::taxonomy-add-placeholder)
                                  :value (get-in data [:new-species-name])
                                  :onChange #(om/update! data :new-species-name
                                                         (.. % -target -value))})
                  (if (empty? (:new-species-name data))
                    (dom/input #js {:type "submit"
                                    :className "btn btn-default input-field-submit"
                                    :onClick #(om/update! data :taxonomy-create-mode false)
                                    :value (tr/translate :words/cancel)})
                    (dom/input #js {:type "submit"
                                    :disabled (if is-valid "" "disabled")
                                    :title (when-not is-valid
                                             (tr/translate ::add-duplicate-species-error))
                                    :className "btn btn-primary input-field-submit"
                                    :onClick #(add-taxonomy-handler data)
                                    :value (tr/translate :words/add)})))))))

(defn taxonomy-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (if (or (empty? (:species data)) (:taxonomy-create-mode data))
        (om/build add-taxonomy-component data)
        (dom/select #js {:className "field-input auto-input"
                         :id "identify-species-select"
                         :value (get-in data [:identification :species])
                         :onChange #(let [v (.. % -target -value)]
                                      (if (= v "create")
                                        (do
                                          (om/update! data :taxonomy-create-mode true)
                                          (.focus (om/get-node owner)))
                                        (do
                                          (om/update! (:identification data) :species v)
                                          (om/update! (:identification data) :dirty-state true))))}
                    (om/build-all species-option-component
                                  (cons {:taxonomy-id -1
                                         :taxonomy-label (str (tr/translate :words/select)
                                                              "...")}
                                        (reverse (conj (into '()
                                                             (sort-by :taxonomy-label
                                                                      (vals (:species data))))
                                                       {:taxonomy-id "create"
                                                        :taxonomy-label (tr/translate ::add-new-species-label)})))
                                  {:key :taxonomy-id}))))))

(defn identify-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "identification-panel"
                    :className (str "identification-panel"
                                    (if (show-panel? data) " show-panel" ""))}
               (dom/div #js {:className "identification-panel-content"}
                        (dom/h4 nil (tr/translate ::identify-selected))
                        (dom/div #js {:className "single-field"}
                                 (dom/label nil (tr/translate :sighting/taxonomy-id.label))
                                 (om/build taxonomy-select-component data))
                        (dom/div nil
                                 (dom/label nil (tr/translate :sighting/sighting-quantity.label))
                                 (dom/input #js {:type "number"
                                                 :className "field-input short-input"
                                                 :value (get-in data [:identification :quantity])
                                                 :onChange #(do
                                                              (om/update! (:identification data) :quantity
                                                                          (cljs.reader/read-string (.. % -target -value)))
                                                              (nav/analytics-event "library-id" "quantity-change"))}))
                        (dom/div #js {:className "flex-row"}
                                 (dom/span #js {:className "field"}
                                           (dom/label nil (tr/translate :sighting/sighting-sex.label))
                                           (om/build sighting-sex-select-component data))
                                 (dom/span #js {:className "field"}
                                           (dom/label nil (tr/translate :sighting/sighting-lifestage.label))
                                           (om/build sighting-lifestage-select-component data))))
               (dom/div #js {:className "field button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :disabled (when (or (not (and (get-in data [:identification :quantity])
                                                                       (get-in data [:identification :species])
                                                                       (> (get-in data [:identification :species]) -1)))
                                                             (:taxonomy-create-mode data))
                                                     "disabled")
                                         :onClick #(do (submit-identification)
                                                       (nav/analytics-event "library-id" "submit-identification"))}
                                    (tr/translate :words/submit))
                        (dom/button #js {:className "btn btn-default"
                                           :onClick #(do (om/update! data :show-identification-panel false)
                                                         (om/update! data [:search :mode] :search)
                                                       (nav/analytics-event "library-id" "cancel-identification"))}
                                      (tr/translate :words/cancel)))))))
