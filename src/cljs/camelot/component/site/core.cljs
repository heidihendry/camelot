(ns camelot.component.site.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]))

(defn site-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"}
               (dom/span #js {:className "menu-item-title"}
                         (:site-name data))
               (dom/span #js {:className "menu-item-description"}
                         (:site-notes data))))))

(defn site-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/sites"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all site-list-component
                                      (sort-by :site-id (:list data))
                                      {:key :site-id}))
               (dom/div #js {:className "sep"})
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do
                                            (nav/nav! "/site/create")
                                            (nav/analytics-event "org-site" "create-click"))
                                :disabled "disabled"
                                :title "Not implemented"}
                           (dom/span #js {:className "fa fa-plus"})
                           " Add Site")
               (dom/button #js {:className "btn btn-default"
                                :onClick #(do (nav/nav! "/sites")
                                              (nav/analytics-event "org-site" "advanced-click"))}
                           "Advanced")))))


