(ns camelot.component.species.update
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.state :as state]
            [camelot.translation.core :as tr]))

(defn navigate-away
  [data]
  (nav/nav! (str "/" (or (get-in (state/app-state-cursor)
                                 [:selected-survey :survey-id :value])
                         "organisation"))))

(defn update-handler
  [data]
  (nav/analytics-event "taxonomy-update" "submit")
  (rest/put-x (str "/taxonomy/" (get-in data [:taxonomy-id :value])),
              {:data (-> data
                         deref
                         (select-keys [:taxonomy-species :taxonomy-genus :taxonomy-family
                                       :taxonomy-order :taxonomy-class :taxonomy-common-name
                                       :species-mass-id :taxonomy-notes])
                         (update-in [:species-mass-id :value] #(if (= % "") nil %)))}
              navigate-away))

(defn blank?
  [d]
  (or (nil? d) (= (.trim d) "")))

(defn validate-form
  [data]
  (or
   (blank? (get-in data [:taxonomy-species :value]))
   (blank? (get-in data [:taxonomy-genus :value]))
   (blank? (get-in data [:taxonomy-common-name :value]))))

(defn submit-button
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [verror (validate-form data)]
        (dom/button #js {:className "btn btn-primary"
                         :disabled (if verror
                                     "disabled" "")
                         :title (when verror
                                  (tr/translate ::validation-error-title))
                         :onClick (partial update-handler data)}
                    (tr/translate :words/update))))))

(defn cancel-button
  "Navigate away without saving the current form state"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-default"
                       :onClick navigate-away}
                  (tr/translate :words/cancel)))))

(defn species-mass-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:species-mass-id data)}
                  (:species-mass-label data)))))

(defn species-mass-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "field-input"
                       :onChange #(om/update! data [:data :species-mass-id :value]
                                              (.. % -target -value))
                       :value (or (get-in data [:data :species-mass-id :value]) "")}
                  (om/build-all species-mass-option-component
                                (conj (into '() (reverse (:species-mass-options data)))
                                      {:species-mass-id ""
                                       :species-mass-label ""})
                                {:key :species-mass-id})))))

(defn text-input-component
  [data owner {:keys [field]}]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:className "field-input"
                      :onChange #(om/update! data [:data field :value]
                                             (.. % -target -value))
                      :value (or (get-in data [:data field :value]) "")}))))

(defn text-area-component
  [data owner {:keys [field]}]
  (reify
    om/IRender
    (render [_]
      (dom/textarea #js {:className "field-input"
                         :rows 3
                         :cols 48
                         :onChange #(om/update! data [:data field :value]
                                                (.. % -target -value))
                         :value (or (get-in data [:data field :value]) "")}))))

(defn form-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label required"}
                          (tr/translate :taxonomy/taxonomy-common-name.label))
               (om/build text-input-component data {:opts {:field :taxonomy-common-name}})
               (dom/label #js {:className "field-label"}
                          (tr/translate :taxonomy/taxonomy-class.label))
               (om/build text-input-component data {:opts {:field :taxonomy-class}})
               (dom/label #js {:className "field-label"}
                          (tr/translate :taxonomy/taxonomy-order.label))
               (om/build text-input-component data {:opts {:field :taxonomy-order}})
               (dom/label #js {:className "field-label"}
                          (tr/translate :taxonomy/taxonomy-family.label))
               (om/build text-input-component data {:opts {:field :taxonomy-family}})
               (dom/label #js {:className "field-label required"}
                          (tr/translate :taxonomy/taxonomy-genus.label))
               (om/build text-input-component data {:opts {:field :taxonomy-genus}})
               (dom/label #js {:className "field-label required"}
                          (tr/translate :taxonomy/taxonomy-species.label))
               (om/build text-input-component data {:opts {:field :taxonomy-species}})
               (dom/label #js {:className "field-label"}
                          (tr/translate :taxonomy/species-mass-id.label))
               (om/build species-mass-select-component data)
               (dom/label #js {:className "field-label"}
                          (tr/translate :taxonomy/taxonomy-notes.label))
               (om/build text-area-component data {:opts {:field :taxonomy-notes}})
               (dom/div #js {:className "button-container"}
                        (om/build cancel-button (:data data))
                        (om/build submit-button (:data data)))))))

(defn update-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x "/species-mass"
                  #(om/update! data :species-mass-options (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (let [v (get-in data [:data :taxonomy-name :value])]
                                      (if (or (nil? v) (= v ""))
                                        (tr/translate ::update-species)
                                        v))))
               (dom/div #js {:className "single-section"}
                        (om/build form-component data))))))
