(ns camelot.component.library.collection
  (:require [om.dom :as dom]
            [camelot.component.library.util :as util]
            [camelot.state :as state]
            [om.core :as om]))

(def collection-columns 3)

(defn updated-select-position
  [media-ids e idx]
  (if (.-ctrlKey e)
    nil
    (if (nil? idx)
      0
      (case (.-keyCode e)
        37 (max (- idx 1) 0)
        38 (if (< idx 3) idx (- idx 3))
        39 (min (+ idx 1) (dec (count media-ids)))
        40 (if (= (.floor js/Math (/ (count media-ids) collection-columns))
                  (.floor js/Math (/ idx collection-columns)))
             idx
             (min (+ idx 3) (dec (count media-ids))))
        65 (max (- idx 1) 0)
        87 (if (< idx 3) idx (- idx 3))
        68 (min (+ idx 1) (dec (count media-ids)))
        83 (if (= (.floor js/Math (/ (count media-ids) collection-columns))
                  (.floor js/Math (/ idx collection-columns)))
             idx
             (min (+ idx 3) (dec (count media-ids))))
        nil))))

(defn handle-key-event
  [data e]
  (let [media-idxs (vec (map-indexed (fn [i e] [i e]) (util/media-ids-on-page data)))
        cur (ffirst (filter #(= (:selected-media-id data) (second %))
                            media-idxs))]
    (if (seq media-idxs)
      (let [id (some->> (updated-select-position media-idxs e cur)
                        (nth media-idxs))
            media (util/find-with-id (second id))]
        (when media
          (when-not (.-shiftKey e)
            (util/deselect-all))
          (om/update! media :selected true)
          (om/update! data :selected-media-id (second id)))))))

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
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-item"}
               (dom/img #js {:className (media-thumb-class data)
                             :onMouseDown #(util/toggle-select-image data (.. % -ctrlKey))
                             :src (str (get-in data [:media-uri]) "/thumb")})
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(om/update! (state/library-state)
                                                   :selected-media-id (:media-id data))})))))


(defn calculate-scroll-update
  [data node]
  (let [media-idxs (vec (map-indexed (fn [i e] [i e]) (util/media-ids-on-page data)))
        cur (ffirst (filter #(= (:selected-media-id data) (second %)) media-idxs))
        row (.floor js/Math (/ cur collection-columns))
        max-row (/ (count media-idxs) collection-columns)
        doc-height (.-scrollHeight node)
        top (.-scrollTop node)
        elt-height (.-clientHeight node)
        bottom (+ elt-height top)
        top-per (/ top doc-height)
        bottom-per (/ bottom doc-height)
        row-per (/ row max-row)
        bot-row-per (/ (+ row 1) max-row)]
    (cond
      (< row-per top-per) (- (* row-per doc-height) (/ doc-height max-row 2))
      (> bot-row-per bottom-per) (- (* bot-row-per doc-height) elt-height)
      :else top)))

(defn media-item-collection-wrapper
  [data owner]
  (reify
    om/IDidMount
    (did-mount
      [this]
      (.focus (om/get-node owner)))
    om/IDidUpdate
    (did-update
      [this prev-props prev-state]
      (let [node (om/get-node owner)]
        (set! (.-scrollTop node) (calculate-scroll-update data node))))
    om/IRender
    (render [_]
      (dom/div #js {:id "media-collection-container"
                    :className "media-collection-container"
                    :tabIndex 1}
      (dom/div nil
               (om/build-all media-item-component (util/media-on-page) {:key :media-id}))))))

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
                                  :id "prev-page"
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
                                  :id "next-page"
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
                         :id "select-all"
                         :disabled (if (get data :identify-selected)
                                     "disabled" "")}
                    "Select None")
        (dom/button #js {:className "btn btn-default search-main-op"
                         :title "Select all media on this page"
                         :id "select-all"
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

(defn media-collection-content-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build subfilter-bar-component data)
               (when (> (count (util/all-media-selected)) 1)
                 (dom/div #js {:className (str "selected-count"
                                               (if (> (get-in data [:search :show-select-count]) 0)
                                                 ""
                                                 " hide-selected"))}
                          (str (count (util/all-media-selected)) " "
                               (get-in data [:search :show-select-action]))))
               (om/build media-item-collection-wrapper data)))))

(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [matches (util/get-matching data)]
        (om/update! (:search data) :match-count (count matches))
        (dom/div #js {:className "media-collection"
                      :onKeyDown #(handle-key-event data %)}
                 (om/build media-collection-content-component data))))))
