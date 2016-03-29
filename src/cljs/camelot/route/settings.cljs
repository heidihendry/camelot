(ns camelot.route.settings
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defn save []
  (throw (js/Error. "Not implemented")))

(defn field-component [[key value] owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div nil
                        (if (map? value)
                          (dom/div nil
                                   (dom/h3 nil (name key))
                                   (apply dom/div nil
                                          (om/build-all field-component value)))
                          (dom/div #js {:className "settings-field"}
                                   (dom/label #js {:className "settings-label"} (name key))
                                   (dom/input #js {:type "text"
                                                   :value value}))))))))

(defn settings-component [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (apply dom/div nil
                      (om/build-all field-component data))
               (dom/button #js {:className "btn btn-primary"
                                         :onClick #(save)}
                                    "Save")))))
