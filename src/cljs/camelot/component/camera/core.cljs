(ns camelot.component.camera.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.util :as util]))

(defn add-success-handler
  [data]
  (om/transact! data :list #(conj % {:camera-name (:new-camera-name data)}))
  (om/update! data :new-camera-name nil))

(defn add-camera-handler
  [data]
  (rest/post-x "/cameras"
               {:data {:camera-name (:new-camera-name data)}}
               (partial add-success-handler data))
  (nav/analytics-event "org-camera" "create-click"))

(defn validate-proposed-camera
  [data]
  (not (some #(= (:new-camera-name data) %)
             (map :camera-name (:list data)))))

(defn add-camera-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [is-valid (validate-proposed-camera data)]
        (dom/form #js {:className "field-input-form"}
                  (dom/input #js {:type "submit"
                                  :disabled (if is-valid "" "disabled")
                                  :title (when-not is-valid
                                           "A camera with this name already exists")
                                  :className "btn btn-primary input-field-submit"
                                  :onClick #(add-camera-handler data)
                                  :value "Add"})
                  (dom/input #js {:className "field-input"
                                  :placeholder "New camera name..."
                                  :value (get-in data [:new-camera-name])
                                  :onChange #(om/update! data :new-camera-name
                                                         (.. % -target -value))}))))))

(defn camera-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed dynamic"}
               (dom/span #js {:className "menu-item-title"}
                         (:camera-name data))
               (dom/span #js {:className "menu-item-description"}
                         (:camera-notes data))))))

(defn camera-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/cameras"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (when (:list data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu"}
                          (if (empty? (:list data))
                            (om/build util/blank-slate-beta-component {}
                                      {:opts {:item-name "cameras"}})
                            (om/build-all camera-list-component
                                          (sort-by :camera-name (:list data))
                                          {:key :camera-id})))
                 (dom/div #js {:className "sep"})
                 (om/build add-camera-component data)
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do
                                              (nav/nav! "/cameras")
                                              (nav/analytics-event "org-camera" "advanced-click"))}
                             "Advanced"))))))


