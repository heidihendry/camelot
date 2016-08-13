(ns camelot.component.species.core
  (:require [om.core :as om]
            [camelot.nav :as nav]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.component.util :as util]))

(defn species-list-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"}
               (dom/span #js {:className "menu-item-title"}
                         (:taxonomy-label data))
               (dom/span #js {:className "menu-item-description"}
                         (when (:taxonomy-common-name data)
                           (dom/div nil
                                  (str "Common Name: " (:taxonomy-common-name data))))
                         (when (:taxonomy-class data)
                           (str "Class: " (:taxonomy-class data) "; "))
                         (when (:taxonomy-order data)
                           (str "Order: " (:taxonomy-order data) "; "))
                         (when (:taxonomy-family data)
                           (str "Family: " (:taxonomy-family data))))))))

(defn species-menu-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/taxonomy"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (when (:list data)
        (dom/div #js {:className "section"}
                 (dom/div #js {:className "simple-menu scroll"}
                          (if (empty? (:list data))
                            (om/build util/blank-slate-beta-component {}
                                      {:opts {:item-name "species"}})
                            (om/build-all species-list-component
                                          (sort-by :taxonomy-label (:list data))
                                          {:key :taxonomy-id})))
                 (dom/div #js {:className "sep"})
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick #(do (nav/nav! "/species/create")
                                                (nav/analytics-event "org-species" "create-click"))
                                  :disabled "disabled"
                                  :title "Not implemented"}
                             (dom/span #js {:className "fa fa-plus"})
                             " Add Species")
                 (dom/button #js {:className "btn btn-default"
                                  :onClick #(do (nav/nav! "/taxonomy")
                                                (nav/analytics-event "org-species" "advanced-click"))}
                             "Advanced"))))))


