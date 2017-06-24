(ns camelot.component.library.sighting-fields
  (:require
   [om.dom :as dom]
   [om.core :as om]
   [camelot.component.library.util :as util]
   [camelot.util.feature :as feature]
   [camelot.state :as state]))

(defn- value-of
  [e]
  (.. e -target -value))

(defn text-input-component
  "Render a text input component for the field"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [field-id (get-in data [::field :sighting-field-id])
            required (get-in data [::field :sighting-field-required])]
        (dom/input #js {:type "text"
                        :className "field-input"
                        :required required
                        :onChange #(om/update! (::identification data) [:sighting-fields field-id] (value-of %))
                        :value (get-in data [::identification :sighting-fields field-id])})))))

(defn select-component-options
  [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value key} value))))

(defn select-component
  "Render a text input component for the field"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [field-id (get-in data [::field :sighting-field-id])
            required (get-in data [::field :sighting-field-required])
            options (get-in data [::field :sighting-field-options])]
        (dom/select #js {:type "text"
                         :className "field-input"
                         :required required
                         :onChange #(om/update! (::identification data) [:sighting-fields field-id] (value-of %))
                         :value (get-in data [::identification :sighting-fields field-id])}
                    (om/build-all select-component-options (conj (map #(vector % %) options) [nil ""])))))))

(defn checkbox-component
  "Render a checkbox component for the field"
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [field-id (get-in data [::field :sighting-field-id])]
        (om/update! (::identification data) [:sighting-fields field-id] false)))
    om/IRender
    (render [_]
      (let [field-id (get-in data [::field :sighting-field-id])
            checked (get-in data [::identification :sighting-fields field-id])]
        (dom/input #js {:type "checkbox"
                        :className "field-input"
                        :checked checked
                        :onChange #(do (om/update! (::identification data) [:sighting-fields field-id]
                                                   (.. % -target -checked)))})))))

(defn number-component
  "Render a number input component for the field"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [field-id (get-in data [::field :sighting-field-id])
            required (get-in data [::field :sighting-field-required])]
        (dom/input #js {:type "number"
                        :className "field-input"
                        :required required
                        :onChange #(om/update! (::identification data) [:sighting-fields field-id] (value-of %))
                        :value (get-in data [::identification :sighting-fields field-id])})))))

(defn textarea-component
  "Render a textarea component for the field"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [field-id (get-in data [::field :sighting-field-id])
            required (get-in data [::field :sighting-field-required])]
        (dom/textarea #js {:className "field-input"
                           :rows 3
                           :cols 50
                           :required required
                           :onChange #(om/update! (::identification data) [:sighting-fields field-id] (value-of %))
                           :value (get-in data [::identification :sighting-fields field-id])})))))

(defn field-component
  "Render a single field with a component appropriate for its datatype."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [required (get-in data [::field :sighting-field-required])]
        (dom/div nil
                 (dom/label #js {:className (str "field-label " (if required "required" ""))}
                            (get-in data [::field :sighting-field-label]))
                 (condp = (get-in data [::field :sighting-field-datatype])
                   :text (om/build text-input-component data)
                   :textarea (om/build textarea-component data)
                   :number (om/build number-component data)
                   :select (om/build select-component data)
                   :checkbox (om/build checkbox-component data)
                   (om/build text-input-component data)))))))

(defn field-data
  "Helper to combine field configuration with form data."
  [data selected-media]
  (let [survey-id (-> selected-media first :survey-id)
        fields (get-in data [:sighting-fields survey-id])]
    (map #(hash-map ::identification (:identification data)
                    ::field %)
         (sort-by (juxt :sighting-field-ordering :sighting-field-label)
                  fields))))

(defn component
  "Top level component for custom sighting fields."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [selected-media (util/all-media-selected data)]
        (when (pos? (count selected-media))
          (dom/div nil
                   (om/build-all field-component (field-data data selected-media))))))))
