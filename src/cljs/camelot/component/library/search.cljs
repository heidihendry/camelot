(ns camelot.component.library.search
  (:require [om.dom :as dom]
            [om.core :as om]
            [camelot.util.filter :as filter]
            [camelot.component.library.util :as util]
            [camelot.state :as state]
            [camelot.rest :as rest]))

(defn add-sighting
  []
  (let [spp (cljs.reader/read-string (get-in (state/library-state) [:identification :species]))
        qty (get-in (state/library-state) [:identification :quantity])
        selected (:selected-media-id (state/library-state))
        all-selected (util/all-media-selected)]
    (rest/put-x "/library/identify" {:data (merge {:identification
                                                   {:quantity qty
                                                    :species spp}}
                                                  {:media
                                                   (map :media-id all-selected)})}
                (fn [resp]
                  (dorun (map #(do (om/update! (second %)
                                               :sightings
                                               (conj (:sightings (second %))
                                                     {:taxonomy-id spp
                                                      :sighting-id (first %)
                                                      :sighting-quantity qty}))
                                   (om/update! (second %) :media-processed true))
                              (zipmap (:body resp) all-selected)))
                  (om/update! (:identification (state/library-state)) :quantity 1)
                  (om/update! (:identification (state/library-state)) :species -1)
                  (om/update! (:search (state/library-state)) :identify-selected false)))))

(defn identify-selected-prompt
  []
  (om/transact! (:search (state/library-state)) :identify-selected not))

(defn submit-identification
  []
  (identify-selected-prompt)
  (add-sighting))

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

(defn survey-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:survey-id data)}
                  (:survey-name data)))))

(defn species-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:taxonomy-id data)}
                  (:taxonomy-label data)))))

(defn filter-button-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:className "fa fa-search btn search"
                       :title "Apply the current filters"
                       :onClick #(om/update! data :dirty-state true)}))))

(defn filter-input-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/input #js {:type "text"
                      :placeholder "Filter..."
                      :title "Type a keyword you want the media to contain"
                      :disabled (if (get data :identify-selected)
                                  "disabled" "")
                      :className "field-input search"
                      :value (get-in data [:search :terms])
                      :onChange #(do (om/update! data :terms (.. % -target -value))
                                     (om/update! data :page 1)
                                     (om/update!  data :dirty-state true)
                                     )}))))

(defn filter-survey-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/select #js {:className "survey-select field-input"
                       :title "Filter to only items in a certain survey"
                       :value (:survey-id data)
                       :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                    (if (> sid -1)
                                      (do
                                        (util/load-library sid)
                                        (util/load-trap-stations sid))
                                      (do
                                        (util/load-library)
                                        (util/load-trap-stations))))}
                  (om/build-all survey-option-component
                                (cons {:survey-id -1 :survey-name "All Surveys"}
                                      (:surveys data))
                                {:key :survey-id})))))

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

(defn identification-panel-button-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/button #js {:className "btn btn-default"
                       :title "Open the identification panel to apply to the selected media"
                       :onClick identify-selected-prompt
                       :disabled (if (or (not (:has-selected state))
                                         (:identify-selected data))
                                   "disabled" "")}
                  "Identify Selected"))))

(defn media-flag-component
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [selected (util/all-media-selected)
            flag-enabled (and (seq selected) (every? (:key state) selected))]
        (dom/span #js {:className ((:classFn state) flag-enabled)
                       :title (:title state)
                       :onClick #((:fn state) (not flag-enabled))})))))

(defn media-flag-container-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/span nil
                (om/build media-flag-component data
                          {:init-state {:title "Flag or unflag the selected media as needing attention"
                                        :key :media-attention-needed
                                        :fn util/set-attention-needed
                                        :classFn #(str "fa fa-2x fa-flag flag"
                                                       (if % " red" ""))}})
                (om/build media-flag-component data
                          {:init-state {:title "Set the selected media as processed or unprocessed"
                                        :key :media-processed
                                        :fn util/set-processed
                                        :classFn #(str "fa fa-2x fa-check processed"
                                                       (if % " green" ""))}})))))

(defn search-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [has-selected (first (filter (comp :selected util/find-with-id)
                                      (get-in data [:search :matches])))]
        (dom/div #js {:className "search-bar"}
                 (om/build filter-button-component (:search data))
                 (om/build filter-input-component (:search data))
                 (dom/span nil " in ")
                 (om/build filter-survey-component data)
                 (om/build pagination-component data)
                 (om/build select-button-components (:search data)
                           {:state {:has-selected has-selected}})
                 (om/build identification-panel-button-component (:search data)
                           {:state {:has-selected has-selected}})
                 (om/build media-flag-container-component data))))))

(defn identification-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "identify-selected"
                                    (if (get-in data [:search :identify-selected])
                                      " show-prompt"
                                      ""
                                      ))}
               (dom/div nil
                        (dom/div #js {:className "field"}
                                 (dom/label nil "Species")
                                 (dom/select #js {:className "field-input"
                                                  :value (get-in data [:identification :species])
                                                  :onChange #(om/update! (:identification data) :species
                                                                         (.. % -target -value))}
                                             (om/build-all species-option-component
                                                           (conj (vals (:species data))
                                                                 {:taxonomy-id -1
                                                                  :taxonomy-species "Select..."})
                                                           {:key :taxonomy-id})))
                        (dom/div #js {:className "field"}
                                 (dom/label nil "Quantity")
                                 (dom/input #js {:type "number"
                                                 :className "field-input"
                                                 :value (get-in data [:identification :quantity])
                                                 :onChange #(when (re-find #"^[0-9]+$"
                                                                           (get-in data [:identification :quantity]))
                                                              (om/update! (:identification data) :quantity
                                                                          (cljs.reader/read-string (.. % -target -value))))}))
                        (dom/div #js {:className "field"}
                                 (dom/button #js {:className "btn btn-primary"
                                                  :disabled (when (not (and (get-in data [:identification :quantity])
                                                                            (get-in data [:identification :species])
                                                                            (> (get-in data [:identification :species]) -1)))
                                                              "disabled")
                                                  :onClick submit-identification} "Submit")
                                 (dom/button #js {:className "btn btn-default"
                                                  :onClick #(om/update! (:search data) :identify-selected false)}
                                             "Cancel")))))))

(defn search-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (:search data) :page 1)
      (om/update! (:search data) :matches (map :media-id (filter/only-matching nil data)))
      (om/update! (:search data) :terms nil))
    om/IRender
    (render [_]
      (when (-> data :search :dirty-state)
        (om/update! (:search data) :dirty-state false)
        (om/update! (:search data) :matches
                    (map :media-id (filter/only-matching (-> data :search :terms) data))))
      (dom/div #js {:className "search-container"}
               (om/build search-bar-component data)
               (om/build identification-bar-component data)))))
