(ns camelot.component.report.core
  "Components for listing, configuring and generating reports."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.rest :as rest]
            [smithy.core :as smithy]
            [camelot.state :as state]))

(defn item-component
  "A menu item for a single report."
  [data owner {:keys [title-key desc-key id-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item"
                    :onClick #(nav/nav! (str "/report/" (get data id-key)))}
               (dom/span #js {:className "menu-item-title"}
                         (title-key data))
               (dom/span #js {:className "menu-item-description"}
                         (desc-key data))))))

(defn configure-report-view
  [data owner {:keys [report-key]}]
  (reify
    om/IWillMount
    (will-mount [_]
      (rest/get-x (str "/report/" report-key)
                  #(om/update! (get (state/app-state-cursor) :view) :content
                               {:screen {:type :report :mode :create :id :report
                                         :resource-id :report
                                         :nav-to true}
                                :screens-ref-override true
                                :buffer {}
                                :screens-ref {:report (assoc (:form (:body %))
                                                             :resource {:type :report
                                                                        :endpoint (str "/report/" report-key "/download")})}
                                :selected-resource {}
                                :generator-data {}})))
    om/IRender
    (render [_]
      (when (:content (get (state/app-state-cursor) :view))
        (om/build (smithy/build-view-component :content) data)))))

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

