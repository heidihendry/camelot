(ns camelot.component.report.core
  "Components for listing, configuring and generating reports."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.nav :as nav]
            [camelot.rest :as rest]
            [smithy.core :as smithy]
            [camelot.state :as state]
            [camelot.component.util :as util]))

(defn item-component
  "A menu item for a single report."
  [data owner {:keys [title-key desc-key id-key]}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "menu-item detailed dynamic"
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
                                :title-override (:title (:body %))
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
        (dom/div #js {:className "split-menu"}
                 (dom/div #js {:className "single-section"}
                        (om/build (smithy/build-view-component :content) data)))))))

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
      (when (:list data)
        (dom/div #js {:className "section"}
                 (dom/div nil
                          (dom/input #js {:className "field-input"
                                          :value (:filter data)
                                          :placeholder "Filter reports..."
                                          :onChange #(om/update! data :filter (.. % -target -value))}))
                 (dom/div #js {:className "simple-menu scroll"}
                          (let [filtered (filter #(if (or (nil? (:filter data)) (empty? (:filter data)))
                                                    true
                                                    (re-matches (re-pattern (str "(?i).*" (:filter data) ".*"))
                                                                (str (:title %) " " (:description %))))
                                                 (sort-by :title (:list data)))]
                            (if (empty? filtered)
                              (om/build util/blank-slate-component {}
                                        {:opts {:item-name "reports"
                                                :notice "No reports matched"
                                                :advice "There weren't any results for this search"}})
                              (om/build-all item-component filtered
                                            
                                            {:opts {:title-key :title
                                                    :id-key :report-key
                                                    :desc-key :description}
                                             :key :report-key})))))))))

