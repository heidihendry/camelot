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


(defn trap-station-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:trap-station-id data)}
                  (:trap-station-name data)))))

(defn trap-station-select-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "subfilter-option"}
               (dom/label #js {} "Trap Station")
               (dom/select #js {:className "trap-station-select field-input"
                                :value (:survey-id data)
                                :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                             (om/update! (:search data) :trap-station-id sid)
                                             (om/update! (:search data) :dirty-state true))}
                           (om/build-all trap-station-option-component
                                         (cons {:trap-station-id -1 :trap-station-name "All Traps"}
                                               (:trap-stations data))
                                         {:key :trap-station-id}))))))

(defn subfilter-checkbox-component
  [data owner]
  (reify
    om/IRenderState
    (render-state
      [_ state]
      (dom/div #js {:className "subfilter-option"}
               (dom/label #js {} (:label state))
               (dom/input #js {:type "checkbox"
                               :value (get-in data [:search (:key state)])
                               :onChange #(do (om/update! (:search (state/library-state))
                                                          (:key state) (.. % -target -checked))
                                              (om/update! (:search data) :dirty-state true))
                               :className "field-input"})))))

(defn subfilter-bar-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "subfilter-bar"}
               (om/build trap-station-select-component data)
               (om/build subfilter-checkbox-component data {:init-state {:key :unprocessed-only
                                                                         :label "Unprocessed"}})
               (om/build subfilter-checkbox-component data {:init-state {:key :flagged-only
                                                                         :label "Flagged"}})))))

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
