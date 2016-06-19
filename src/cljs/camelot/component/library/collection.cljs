(ns camelot.component.library.collection
  (:require [om.dom :as dom]
            [camelot.component.library.util :as util]
            [camelot.state :as state]
            [om.core :as om]))

(defn- media-thumb-class
  [data]
  (str "media"
       (if (:selected data) " selected" "")
       (cond
         (:media-attention-needed data) " attention-needed"
         (:media-processed data) " processed"
         :else "")))

(defn media-item-component
  "Render a single library item."
  [result owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-item"}
               (dom/img #js {:className (media-thumb-class result)
                             :onMouseDown #(util/toggle-select-image result (.. % -ctrlKey))
                             :src (str (get-in result [:media-uri]) "/thumb")})
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(om/update! (state/library-state)
                                                   :selected-media-id (:media-id result))})))))

(defn prev-page
  [page]
  (if (= page 1)
    page
    (do
      (dec page))))

(defn next-page
  [matches page]
  (if (= (.ceil js/Math (/ matches util/page-size)) page)
    page
    (inc page)))

(defn pagination-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [matches (get-in data [:search :match-count])]
        (dom/div #js {:className "pagination-nav"}
                 (dom/button #js {:className "fa fa-2x fa-angle-left btn btn-default"
                                  :disabled (if (get-in data
                                                        [:search :identify-selected])
                                              "disabled" "")
                                  :onClick #(do (util/deselect-all)
                                                (om/transact! data [:search :page] prev-page))})
                 (dom/div #js {:className "describe-pagination"}
                          (str (+ (- (* util/page-size (get-in data [:search :page])) util/page-size) 1)
                               " - "
                               (min (* util/page-size (get-in data [:search :page])) matches)
                               " of "
                               matches))
                 (dom/button #js {:className "fa fa-2x fa-angle-right btn btn-default"
                                  :disabled (if (get-in data
                                                        [:search :identify-selected])
                                              "disabled" "")
                                  :onClick #(do (util/deselect-all)
                                                (om/transact! data [:search :page] (partial next-page matches)))}))))))

(defn select-button-components
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (if (:has-selected state)
        (dom/button #js {:className "btn btn-default search-main-op"
                         :onClick util/deselect-all*
                         :title "Remove all selections"
                         :disabled (if (get data :identify-selected)
                                     "disabled" "")}
                    "Select None")
        (dom/button #js {:className "btn btn-default search-main-op"
                         :title "Select all media on this page"
                         :disabled (if (get data :identify-selected)
                                     "disabled" "")
                         :onClick util/select-all*}
                    "Select All")))))

(defn subfilter-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [has-selected (first (filter (comp :selected util/find-with-id)
                                        (get-in data [:search :matches])))]
        (dom/div #js {:className "subfilter-bar"}
                 (om/build pagination-component data)
                 (om/build select-button-components (:search data)
                           {:state {:has-selected has-selected}}))))))


(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [matches (util/get-matching data)]
        (om/update! (:search data) :match-count (count matches))
        (dom/div #js {:className "media-collection"}
                 (om/build subfilter-bar-component data)
                 (when (> (count (util/all-media-selected)) 1)
                   (dom/div #js {:className (str "selected-count"
                                                 (if (> (get-in data [:search :show-select-count]) 0)
                                                   ""
                                                   " hide-selected"))}
                            (str (count (util/all-media-selected)) " selected")))
                 (dom/div #js {:className "media-collection-container"}
                          (om/build-all media-item-component (util/media-on-page) {:key :media-id})))))))
