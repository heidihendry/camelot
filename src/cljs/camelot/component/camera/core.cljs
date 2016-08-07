(ns camelot.component.camera.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.util :as util]))

(defn camera-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"}
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
                                          (sort-by :camera-id (:list data))
                                          {:key :camera-id})))
                 (dom/div #js {:className "sep"})
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(do
                                              (nav/nav! "/camera/create")
                                              (nav/analytics-event "org-camera" "create-click"))
                                  :disabled "disabled"
                                  :title "Not implemented"}
                             (dom/span #js {:className "fa fa-plus"})
                             " Add Camera")
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do
                                              (nav/nav! "/cameras")
                                              (nav/analytics-event "org-camera" "advanced-click"))}
                             "Advanced"))))))


