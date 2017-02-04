(ns camelot.component.library.collection
  (:require [om.dom :as dom]
            [camelot.component.library.util :as util]
            [camelot.component.util :as cutil]
            [camelot.state :as state]
            [om.core :as om]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]
            [clojure.string :as str]))

(def collection-columns 3)

(defn updated-select-position
  [media-ids e idx]
  (if (.-ctrlKey e)
    nil
    (if (nil? idx)
      0
      (case (.-keyCode e)
        37 (do (.preventDefault e)
               (nav/analytics-event "library-key" "<left>")
               (max (- idx 1) 0))
        38 (do (.preventDefault e)
               (nav/analytics-event "library-key" "<up>")
               (if (< idx 3) idx (- idx 3)))
        39 (do (.preventDefault e)
               (nav/analytics-event "library-key" "<right>")
               (min (+ idx 1) (dec (count media-ids))))
        40 (do (.preventDefault e)
               (nav/analytics-event "library-key" "<down>")
               (if (= (.floor js/Math (/ (count media-ids) collection-columns))
                      (.floor js/Math (/ idx collection-columns)))
                 idx
                 (min (+ idx 3) (dec (count media-ids)))))
        65 (do
             (nav/analytics-event "library-key" "a")
             (max (- idx 1) 0))
        87 (do
             (nav/analytics-event "library-key" "w")
             (if (< idx 3) idx (- idx 3)))
        68 (do
             (nav/analytics-event "library-key" "s")
             (min (+ idx 1) (dec (count media-ids))))
        83 (do
             (nav/analytics-event "library-key" "d")
             (if (= (.floor js/Math (/ (count media-ids) collection-columns))
                    (.floor js/Math (/ idx collection-columns)))
               idx
               (min (+ idx 3) (dec (count media-ids)))))
        nil))))

(defn handle-key-event
  [data e]
  (cond
    (and (= (.-keyCode e) 70) (not (.-ctrlKey e)))
    (do
      (.click (.getElementById js/document "media-flag"))
      (nav/analytics-event "library-key" "f"))

    (and (= (.-keyCode e) 71) (not (.-ctrlKey e)))
    (do
      (.click (.getElementById js/document "media-processed"))
      (nav/analytics-event "library-key" "g"))

    (and (= (.-keyCode e) 82) (not (.-ctrlKey e)))
    (do
      (.click (.getElementById js/document "media-reference-quality"))
      (nav/analytics-event "library-key" "r"))

    (and (= (.-keyCode e) 67) (not (.-ctrlKey e)))
    (do
      (.click (.getElementById js/document "media-cameracheck"))
      (nav/analytics-event "library-key" "c"))

    (and (= (.-keyCode e) 65) (.-ctrlKey e))
    (do
      (.preventDefault e)
      (.click (.getElementById js/document "select-all"))
      (nav/analytics-event "library-key" "C-a"))

    :else
    (let [media-idxs (vec (map-indexed (fn [i e] [i e]) (util/media-ids-on-page data)))
          endpoint-idx (ffirst (filter #(= (:selected-media-id data) (second %)) media-idxs))
          new-endpoint (updated-select-position media-idxs e endpoint-idx)]
      (when new-endpoint
        (if (and (.-shiftKey e) (:anchor-media-id data))
          (let [anchor-idx (ffirst (filter #(= (:anchor-media-id data) (second %)) media-idxs))
                first-idx (min anchor-idx new-endpoint)
                last-idx (max anchor-idx new-endpoint)
                media-in-range (->> media-idxs
                                    (drop first-idx)
                                    (take (inc (- last-idx first-idx)))
                                    (map second)
                                    (map util/find-with-id))]
            (util/deselect-all)
            (dorun (map #(om/update! % :selected true) media-in-range))
            (om/update! data :selected-media-id (second (nth media-idxs new-endpoint)))
            (util/show-select-message))
          (let [id (second (nth media-idxs new-endpoint))]
            (util/deselect-all)
            (om/update! (util/find-with-id id) :selected true)
            (om/update! data :selected-media-id id)
            (om/update! data :anchor-media-id id)))))))

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
               (dom/div #js {:className (media-thumb-class data)}
                        (dom/img #js {:onMouseDown #(do
                                                      (util/toggle-select-image data (.. % -ctrlKey))
                                                      (nav/analytics-event "library-collection" "select-media"))
                                      :src (str (get-in data [:media-uri]) "/thumb")}))
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(do
                                         (om/update! (state/library-state) :selected-media-id (:media-id data))
                                         (nav/analytics-event "library-collection" "view-media"))})))))


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

(defn media-tips-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build cutil/blank-slate-component data
                {:opts {:item-name (tr/translate ::item-name)
                        :advice (tr/translate ::upload-advice)}}))))

(defn filter-blank-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build cutil/blank-slate-component data
                {:opts {:notice (tr/translate ::filter-notice)
                        :advice (tr/translate ::filter-advice)}}))))

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
      (let [ms (util/media-on-page)]
        (dom/div #js {:id "media-collection-container"
                      :className "media-collection-container"
                      :tabIndex 1}
                 (cond
                   (empty? (get-in (state/library-state) [:search :results]))
                   (om/build media-tips-component data)

                   (empty? ms)
                   (om/build filter-blank-component data)

                   :else (dom/div #js {:className "media-item-wrapper"}
                                  (om/build-all media-item-component ms
                                                {:key :media-id}))))))))

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

(defn thousands-sep
  [n]
  (->> n
       str
       seq
       reverse
       (partition 3 3 nil)
       reverse
       (map #(apply str (reverse %)))
       (clojure.string/join ",")
       (apply str)))

(defn pagination-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [match-count (get-in data [:search-results :total-matches])]
        (dom/div #js {:className "pagination-nav"}
                 (dom/button #js {:className "fa fa-2x fa-angle-left btn btn-default"
                                  :disabled (if (or (get-in data [:search :identify-selected])
                                                    (= (- (* util/page-size (get-in data [:search :page])) util/page-size) 0))
                                              "disabled" "")
                                  :id "prev-page"
                                  :onClick #(do (util/deselect-all)
                                                (om/transact! data [:search :page] prev-page)
                                                (nav/analytics-event "library-collection" "prev-page-click"))})
                 (dom/div #js {:className "describe-pagination"}
                          (str (thousands-sep (+ (- (* util/page-size (get-in data [:search :page])) util/page-size) 1))
                               " - "
                               (thousands-sep (min (* util/page-size (get-in data [:search :page])) match-count))
                               " of "
                               (thousands-sep match-count)))
                 (dom/button #js {:className "fa fa-2x fa-angle-right btn btn-default"
                                  :disabled (if (or (get-in data [:search :identify-selected])
                                                    (>= (* util/page-size (get-in data [:search :page])) match-count))
                                              "disabled" "")
                                  :id "next-page"
                                  :onClick #(do (util/deselect-all)
                                                (om/transact! data [:search :page] (partial next-page match-count))
                                                (nav/analytics-event "library-collection" "next-page-click"))}))))))

(defn select-button-components
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (if (:all-selected state)
        (dom/button #js {:className "btn btn-default search-main-op"
                         :onClick #(do (util/deselect-all*)
                                       (nav/analytics-event "library-collection" "deselect-all-click"))
                         :title (tr/translate ::select-none-button-title)
                         :id "select-all"
                         :disabled (if (get data :identify-selected)
                                     "disabled" "")}
                    (tr/translate ::select-none-button))
        (dom/button #js {:className "btn btn-default search-main-op"
                         :title (tr/translate ::select-all-button-title)
                         :id "select-all"
                         :disabled (if (get data :identify-selected)
                                     "disabled" "")
                         :onClick #(do (util/select-all*)
                                       (nav/analytics-event "library-collection" "select-all-click"))}
                    (tr/translate ::select-all-button))))))

(defn subfilter-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [all-selected (every? :selected (util/media-on-page data))]
        (dom/div #js {:className "subfilter-bar"}
                 (om/build pagination-component data)
                 (om/build select-button-components (:search data)
                           {:state {:all-selected all-selected}}))))))

(defn media-collection-content-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-collection-content-wrapper"}
               (om/build subfilter-bar-component data)
               (when (or (> (count (util/all-media-selected)) 1)
                         (get-in data [:search :show-select-count-override]))
                 (dom/div #js {:className (str "selected-count"
                                               (if (or (> (get-in data [:search :show-select-count]) 0)
                                                       (get-in data [:search :show-select-count-override]))
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
      (dom/div #js {:className "media-collection"
                    :onKeyDown #(handle-key-event data %)}
               (om/build media-collection-content-component data)))))
