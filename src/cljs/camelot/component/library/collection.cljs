(ns camelot.component.library.collection
  (:require [om.dom :as dom]
            [camelot.component.library.util :as util]
            [camelot.component.util :as cutil]
            [camelot.state :as state]
            [om.core :as om]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]
            [clojure.string :as str]
            [camelot.component.library.identify :as identify]))

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
      (let [media-id (:media-id (:media data))]
        (dom/div #js {:className "media-item"}
                 (dom/div #js {:className (media-thumb-class (:media data))}
                          (dom/img #js {:onMouseDown #(do
                                                        (util/toggle-select-image (:data data) media-id %)
                                                        (nav/analytics-event "library-collection" "select-media"))
                                        :onDragOver #(do
                                                       (let [mids (util/media-ids-on-page (:data data))
                                                             idxs (vec (map-indexed (fn [i e] [i e]) mids))
                                                             endpoint (.indexOf (vec mids) media-id)]
                                                         (when-not (= (:drag-endpoint-media-id (:data data)) media-id)
                                                           (om/update! (:data data) :drag-endpoint-media-id media-id)
                                                           (util/apply-selection-range (:data data) idxs endpoint true false))))
                                        :onDragStart #(do
                                                        (om/update! (state/library-state) :selected-media-id media-id)
                                                        (om/update! (state/library-state) :anchor-media-id media-id)
                                                        (om/update! (state/library-state) :drag-endpoint-media-id nil)
                                                        (nav/analytics-event "library-collection" "drag-media"))
                                        :src (str (get-in (:media data) [:media-uri]) "/thumb")}))
                 (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                               :onClick #(do
                                           (om/update! (state/library-state) :selected-media-id media-id)
                                           (nav/analytics-event "library-collection" "view-media"))}))))))


(defn calculate-scroll-update
  [data node]
  (let [identification-panel-clearance 70
        media-idxs (vec (map-indexed (fn [i e] [i e]) (util/media-ids-on-page data)))
        cur (ffirst (filter #(= (:selected-media-id data) (second %)) media-idxs))
        row (.floor js/Math (/ cur util/collection-columns))
        max-row (/ (count media-idxs) util/collection-columns)
        doc-height (.-scrollHeight node)
        top (.-scrollTop node)
        elt-height (- (.-clientHeight node) identification-panel-clearance)
        bottom (+ elt-height top)
        top-per (/ top doc-height)
        bottom-per (/ bottom doc-height)
        row-per (/ row max-row)
        bot-row-per (/ (+ row 1) max-row)]
    (cond
      (< row-per top-per) (- (* row-per doc-height) (/ doc-height max-row 2))
      (> bot-row-per bottom-per) (- (* bot-row-per doc-height) elt-height)
      :else top)))

(defn reference-window-media-tips-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (om/build cutil/blank-slate-component data
                {:opts {:notice (tr/translate ::reference-window-no-media-notice)
                        :advice (tr/translate ::reference-window-no-media-advice)}}))))

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
        (when-not (= (:selected-media-id prev-props)
                     (:selected-media-id data))
          (set! (.-scrollTop node) (calculate-scroll-update data node)))))
    om/IRender
    (render [_]
      (if-let [ms (util/media-on-page data)]
        (do
          (dom/div #js {:id "media-collection-container"
                        :className "media-collection-container"
                        :tabIndex 1}
                   (cond
                     (and (:restricted-mode (deref (state/app-state-cursor)))
                          (get-in data [:search :inprogress]))
                     (dom/div #js {:className "align-center"}
                              (dom/img #js {:className "spinner"
                                            :src "images/spinner.gif"
                                            :height "32"
                                            :width "32"}))

                     (and (:restricted-mode (deref (state/app-state-cursor)))
                          (empty? (:records (state/library-state))))
                     (om/build reference-window-media-tips-component data)

                     (empty? (:records (state/library-state)))
                     (om/build media-tips-component data)

                     (empty? ms)
                     (om/build filter-blank-component data)

                     :else (dom/div #js {:className "media-item-wrapper"}
                                    (when (seq (:records data))
                                      (om/build-all media-item-component
                                                    (map #(hash-map :data data
                                                                    :media %)
                                                         (util/media-on-page data))
                                                    {:key-fn #(get-in % [:media :media-id])}))))))
        (dom/div #js {:id "media-collection-container"
                      :className "media-collection-container"
                      :tabIndex 1})))))

(defn indices-for-page
  [pp match-count]
  {:start (* util/page-size (dec pp))
   :end (dec (min (* util/page-size pp) match-count))})

(defn hydrate-for-page
  [data pp match-count cb]
  (let [idxs (indices-for-page pp match-count)]
    (util/hydrate-media data
                        (->> (get-in data [:search :ordered-ids])
                             (drop (:start idxs))
                             (take util/page-size))
                        (:metadata @data)
                        cb)))

(defn prev-page
  [data match-count]
  (let [page (get-in data [:search :page])
        newpage (if (= page 1)
                  page
                  (dec page))]
    (when-not (= page newpage)
      (hydrate-for-page data newpage match-count
                        #(do (om/update! % [:search :page] newpage)
                             (util/deselect-all %))))))

(defn next-page
  [data match-count]
  (let [page (get-in data [:search :page])
        newpage (if (= (.ceil js/Math (/ match-count util/page-size)) page)
                  page
                  (inc page))]
    (when-not (= page newpage)
      (hydrate-for-page data newpage match-count
                        #(do (om/update! % [:search :page] newpage)
                             (util/deselect-all %))))))

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
      (let [match-count (count (get-in data [:search :ordered-ids]))]
        (dom/div #js {:className "pagination-nav"}
                 (dom/button #js {:className "fa fa-2x fa-angle-left btn btn-default"
                                  :disabled (if (or (get-in data [:search :identify-selected])
                                                    (= (- (* util/page-size (get-in data [:search :page])) util/page-size) 0))
                                              "disabled" "")
                                  :id "prev-page"
                                  :onClick #(do (prev-page (state/library-state) match-count)
                                                (nav/analytics-event "library-collection" "prev-page-click"))})
                 (let [idxs (indices-for-page (get-in data [:search :page]) match-count)]
                   (dom/div #js {:className "describe-pagination"}
                            (str (thousands-sep (inc (:start idxs)))
                                 " - "
                                 (thousands-sep (inc (:end idxs)))
                                 " of "
                                 (thousands-sep match-count))))
                 (dom/button #js {:className "fa fa-2x fa-angle-right btn btn-default"
                                  :disabled (if (or (get-in data [:search :identify-selected])
                                                    (>= (* util/page-size (get-in data [:search :page])) match-count))
                                              "disabled" "")
                                  :id "next-page"
                                  :onClick #(do (next-page (state/library-state) match-count)
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
               (let [{:keys [num description visible]} (:notification data)]
                 (dom/div #js {:className (str "selected-count"
                                               (if visible
                                                 ""
                                                 " hide-selected"))}
                          (str num " " description)))
               (om/build media-item-collection-wrapper data)))))

(defn show-identification-bar?
  [data]
  (pos? (count (util/all-media-selected data))))

(defn single-survey?
  [data]
  (= (count (util/selection-survey data)) 1))

(defn identify-selection-bar
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "identify-selection-bar"
                                    (if (show-identification-bar? data)
                                      " show-selection-bar"
                                      ""))}
               (dom/button #js {:className "btn btn-primary"
                                :id "identify-selected"
                                :title (cond
                                         (not (single-survey? data))
                                         (tr/translate ::selected-media-from-multiple-surveys)

                                         :default nil)
                                :disabled (if (or (zero? (count (util/all-media-selected data)))
                                                  (not (single-survey? data))) "disabled" "")
                                :onClick #(om/transact! data :show-identification-panel not)}
                           (tr/translate ::identify-selected))))))

(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (do
        (when (:deferred-hydrate data)
          (util/hydrate-media data (util/media-ids-on-page data) (:metadata data))
          (om/update! data :deferred-hydrate nil))
        (dom/div #js {:className "media-collection"}
                 (om/build media-collection-content-component data)
                 (om/build identify-selection-bar data))))))
