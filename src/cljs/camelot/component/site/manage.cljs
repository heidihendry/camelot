(ns camelot.component.site.manage
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]))

(defn form-textarea
  [data owner {:keys [label value-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"} label)
               (dom/textarea #js {:className "field-input"
                                  :rows 3
                                  :cols 48
                                  :onChange #(om/update! data [value-key :value] (.. % -target -value))
                                  :value (get-in data [value-key :value])})))))

(defn form-input
  [data owner {:keys [label value-key required validator warning]}]
  (reify
    om/IRender
    (render [_]
      (let [vr (if validator (validator) true)]
        (when validator
          (om/update! data :validation-failure (not vr)))
        (dom/div nil
                 (dom/label #js {:className (str "field-label" (when required " required"))} label)
                 (dom/input #js {:className "field-input"
                                 :placeholder (str label "...")
                                 :onChange #(om/update! data [value-key :value] (.. % -target -value))
                                 :value (get-in data [value-key :value])})
                 (when-not vr
                   (dom/div #js {:className "validation-warning"} warning)))))))

(defn form-layout
  [data]
  [[(tr/translate :concepts/site-name)
    :site-name :text-input {:required true
                            :validator (fn [] (let [v (get-in data [:data :site-name :value])]
                                                (not (or (nil? v) (= "" v)
                                                         (some #(= v %) (map :site-name (:list data)))))))
                            :warning (tr/translate ::validation-site-name)}]
   [(tr/translate :concepts/sublocation) :site-sublocation :text-input {}]
   [(tr/translate :concepts/nearest-city) :site-city :text-input {}]
   [(tr/translate :concepts/state-province) :site-state-province :text-input {}]
   [(tr/translate :concepts/country) :site-country :text-input {}]
   [(tr/translate :concepts/area-covered) :site-area :text-input {}]
   [(tr/translate :words/notes) :site-notes :textarea {}]])

(defn update-success-handler
  [data]
  (nav/nav-up! 2))

(defn update-handler
  [data]
  (nav/analytics-event "site-update" "submit")
  (rest/put-x (str "/sites/" (get-in data [:site-id :value])),
              {:data (select-keys (deref data) [:site-name :site-sublocation :site-city
                                                :site-state-province :site-country
                                                :site-area :site-notes])}
              update-success-handler))

(defn submit-button
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-primary"
                       :disabled (if (:validation-failure data)
                                   "disabled" "")
                       :title (when (:validation-failure data)
                                (tr/translate ::validation-failure))
                       :onClick (partial update-handler data)}
                  (tr/translate :words/update)))))

(defn form-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (map #(om/build (if (= (nth % 2) :textarea)
                                 form-textarea
                                 form-input)
                               (:data data)
                               {:opts (merge (nth % 3)
                                             {:label (first %)
                                              :value-key (second %)})})
                    (form-layout data))
               (om/build submit-button (:data data))))))

(defn manage-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "intro"}
                        (dom/h4 nil (let [v (get-in data [:data :site-name :value])]
                                      (if (or (nil? v) (= v ""))
                                        (tr/translate ::default-intro)
                                        v))))
               (dom/div #js {:className "single-section"}
                        (om/build form-component data))))))
