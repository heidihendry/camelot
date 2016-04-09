(ns camelot.component.settings
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.component.albums :as albums]
            [camelot.component.inputs :as inputs]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [camelot.rest :as rest]))

(defn save []
  (do
    (om/update! (state/app-state-cursor) :config
                (deref (state/config-buffer-state)))
    (rest/post-settings {:config (deref (state/config-state))}
                        (fn [d] (nav/toggle-settings!)
                          (albums/reload-albums)))))

(defn cancel []
  (do
    (om/update! (state/app-state-cursor) :config-buffer
                (deref (state/config-state)))
    (nav/toggle-settings!)))

(defn field-component
  [[menu-item s] owner]
  (reify
    om/IRender
    (render [_]
      (if (= (first menu-item) :label)
        (dom/h4 #js {:className "section-heading"} (second menu-item))
        (let [value (get (state/settings-config-state) (first menu-item))]
          (dom/div #js {:className "settings-field"}
                   (dom/label #js {:className "settings-label"
                                   :title (:description value)} (:label value))
                   (om/build inputs/input-field
                             [(first menu-item) value s])))))))

(defn settings-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:id "settings-inner"}
             (om/build-all field-component
                           (map #(vector % (:config-state data))
                                (:menu data)))))))

(defn settings-view-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h4 nil "Settings")
               (dom/div nil (om/build settings-component
                                      {:menu (:menu (:settings app))
                                       :config-state (state/config-buffer-state)}))
               (dom/div #js {:className "button-container"}
                        (dom/button #js {:className "btn btn-primary"
                                         :onClick #(save)}
                                    "Save")
                        (dom/button #js {:className "btn btn-default"
                                         :onClick #(cancel)}
                                    "Cancel"))))))
