(ns camelot.route.settings
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defn save []
  (throw (js/Error. "Not implemented")))

(defn select-option-component
  [[key desc] owner]
  (prn (type key))
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (if (= (type key) cljs.core/Keyword)
                                (name key)
                                key)} desc))))

(defmulti input-field :type)

(defmethod input-field :select
  [s owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:value (if (= (type key) cljs.core/Keyword)
                                (name (first (keys (:options s))))
                                (first (keys (:options s))))}
                  (om/build-all select-option-component (:options s))))))

(defmethod input-field :default
  [s owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "text" :value (name (:type s))}))))

(defn field-component
  [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div nil
                        (dom/div #js {:className "settings-field"}
                                 (dom/label #js {:className "settings-label"
                                                 :title (:description value)} (:label value))
                                 (om/build input-field (:schema value))))))))

(defn settings-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (apply dom/div nil
                      (om/build-all field-component data))
               (dom/button #js {:className "btn btn-primary"
                                         :onClick #(save)}
                                    "Save")))))
