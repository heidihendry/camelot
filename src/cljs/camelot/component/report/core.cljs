(ns camelot.component.report.core
  "Components for listing, configuring and generating reports."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.rest :as rest]))

(defn item-component
  "A menu item for a single report."
  [data owner {:keys [title-key desc-key id-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed"
                    :onClick #(nav/nav! (str "/report/" (get data id-key)))}
               (dom/span #js {:className "menu-item-title"}
                         (title-key data))
               (dom/span #js {:className "menu-item-description"}
                         (desc-key data))))))

(defn configure-report-view
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil "Report Configured Here"))))

(defn menu-component
  "List all reports."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-resource "/report"
                         #(om/update! data :list (:body %))))
    om/IRender
    (render [_]
      (dom/div #js {:className "section"}
               (dom/div #js {:className "simple-menu"}
                        (om/build-all item-component (sort-by :title (:list data))
                                      {:opts {:title-key :title
                                              :id-key :report-key
                                              :desc-key :description}
                                       :key :report-key}))))))

