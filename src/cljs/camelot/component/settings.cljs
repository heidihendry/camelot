(ns camelot.component.settings
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-datepicker.components :refer [datepicker]]
            [camelot.state :as state]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defn save []
  (throw (js/Error. "Not implemented")))

(defn cancel []
  (throw (js/Error. "Not implemented")))

(defn add []
  (throw (js/Error. "Not implemented")))

(defn select-option-component
  [[key desc] owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (if (= (type key) cljs.core/Keyword)
                                (name key)
                                key)} desc))))

(defmulti input-field (fn [[k v]] (:type (:schema v))))

(defmethod input-field :select
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (let [val (get-in (state/config-state) [k :value])]
        (dom/select #js {:className "settings-input" :value
                         (if (= (type val) cljs.core/Keyword)
                           (name val)
                           val)}
                    (om/build-all select-option-component (:options (:schema v))))))))

(defn string-list-item
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "list-item"} data))))

(defn path-list-item
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/span #js {:className "list-item"}
                (get (state/metadata-schema-state) data)
                (dom/span #js {:className "list-item-delete fa fa-trash"})))))

(defmethod input-field :list
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "list-input"}
               (apply dom/div nil
                      (if (= (:list-of (:schema v)) :paths)
                        (om/build-all path-list-item (sort #(< (get (state/metadata-schema-state) %1)
                                              (get (state/metadata-schema-state) %2)) (get-in (state/config-state) [k :value])))
                        (om/build-all string-list-item (sort #(< %1 %2) (get-in (state/config-state) [k :value])))))
               (if (= (:complete-with (:schema v)) :metadata)
                 (dom/select #js {:className "settings-input"}
                             (om/build-all select-option-component
                                           (sort #(< (second %1) (second %2)) (remove #(some (set %) (get-in (state/config-state) [k :value]))
                                                                (state/metadata-schema-state)))))
                 (dom/input #js {:type "text" :className "settings-input" :placeholder "Add item"}))
               (dom/button #js {:className "btn btn-primary fa fa-plus fa-2x"
                                :onClick #(add)})))))

(defmethod input-field :datetime
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (om/build datepicker (get (state/config-state) k)))))

;; TODO this needs to be between 0.0 and 1.0
(defmethod input-field :percentage
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "settings-input" :value (get-in (state/config-state) [k :value])}))))

(defmethod input-field :number
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "number" :className "settings-input" :value (get-in (state/config-state) [k :value])}))))

(defmethod input-field :default
  [[k v] owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "text" :className "settings-input" :value (get-in (state/config-state) [k :value])}))))

(defn field-component
  [menu-item owner]
  (prn menu-item)
  (reify
    om/IRender
    (render [_]
      (if (= (first menu-item) :label)
        (dom/h4 #js {:className "section-heading"} (second menu-item))
        (let [value (get (state/settings-config-state) (first menu-item))]
          (dom/div #js {:className "settings-field"}
                   (dom/label #js {:className "settings-label"
                                   :title (:description value)} (:label value))
                   (om/build input-field [(first menu-item) value])))))))

(defn settings-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:id "settings-inner"}
             (om/build-all field-component data)))))

(defn settings-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil "Settings")
               (dom/div nil (om/build settings-component (:menu (:settings app))))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(save)}
                                    "Save")
                        (dom/button #js {:className "btn btn-default"
                                         :onClick #(cancel)}
                                    "Cancel"))))))
