(ns camelot.component.camera.manage
  (:require [om.core :as om]
            [camelot.rest :as rest]
            [om.dom :as dom]
            [camelot.nav :as nav]))

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
                                  :onChange #(om/update! data value-key (.. % -target -value))
                                  :value (get-in data [value-key :value])})))))

(defn form-input
  [data owner {:keys [label value-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label #js {:className "field-label"} label)
               (dom/input #js {:className "field-input"
                               :placeholder (str label "...")
                               :onChange #(om/update! data [value-key :value] (.. % -target -value))
                               :value (get-in data [value-key :value])})))))

(def form-layout
  [["Camera name" :camera-name :text-input]
   ["Camera make" :camera-make :text-input]
   ["Camera model" :camera-model :text-input]
   ["Camera notes" :camera-notes :textarea]])

(defn update-success-handler
  [data]
  (nav/nav-up! 2))

(defn update-handler
  [data]
  (nav/analytics-event "camera-update" "submit")
  (prn (select-keys data [:camera-id :camera-name :camera-make
                          :camera-model :camera-notes
                          :camera-status-id]))
  (rest/put-x (str "/cameras/" (get-in data [:camera-id :value])),
              {:data (select-keys (deref data) [:camera-name :camera-make
                                                :camera-model :camera-notes
                                                :camera-status-id])}
              update-success-handler))

(defn submit-button
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "btn btn-primary"
                       :onClick (partial update-handler data)}
                  "Update"))))

(defn form-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (map #(om/build (if (= (nth % 2) :textarea)
                                 form-textarea
                                 form-input)
                               data {:opts {:label (first %)
                                            :value-key (second %)}})
                    form-layout)
               (om/build submit-button data)))))

(defn manage-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "split-menu"}
               (dom/div #js {:className "single-section"}
                        (dom/h4 nil (get-in data [:camera-name :value]))
                        (om/build form-component data))))))
