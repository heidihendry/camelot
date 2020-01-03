(ns camelot.component.library.preview
  "Component for Library media preview with details panel."
  (:require [goog.object :as object]
            [om.dom :as dom]
            [camelot.state :as state]
            [om.core :as om]
            [camelot.component.library.util :as util]
            [camelot.util.sighting-fields :as util.sf]
            [camelot.rest :as rest]
            [camelot.nav :as nav]
            [clojure.string :as str]
            [camelot.translation.core :as tr])
  (:import [goog.i18n DateTimeFormat]))

(def photo-not-selected (tr/translate ::photo-not-selected))

(defn remove-sighting
  [data sighting-id]
  (let [selected (util/find-with-id data (:selected-media-id (state/library-state)))]
    (om/update! selected :sightings
                (filterv (fn [s] (not= sighting-id (:sighting-id s)))
                         (:sightings selected)))
    (rest/delete-resource (str "/sightings/" sighting-id) {} identity)))

(def preview-adjustment-scale
  (.toFixed (.log js/Math 10) 8))

(defn preview-adjustment-slider
  [{:keys [label value update!]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "slider"}
               (dom/label nil label)
               (dom/input #js {:type "range"
                               :min (* -1 preview-adjustment-scale)
                               :max preview-adjustment-scale
                               :step (/ preview-adjustment-scale 200)
                               :value value
                               :onClick #(.preventDefault %)
                               :onChange update!})))))

(defn exp-perc
  [n]
  (str (int (* (.exp js/Math n) 100)) "%"))

(defn preview-adjustment-panel
  [{:keys [brightness update-brightness! contrast update-contrast!]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "preview-adjustment-panel"}
               (dom/div #js {:className "panel-background"})
               (dom/div #js {:className "input-container"}
                        (om/build preview-adjustment-slider
                                  {:label (str (tr/translate ::brightness-label) " (" (exp-perc brightness) ")")
                                   :value brightness
                                   :update! update-brightness!})
                        (om/build preview-adjustment-slider
                                  {:label (str (tr/translate ::contrast-label) " (" (exp-perc contrast) ")")
                                   :value contrast
                                   :update! update-contrast!}))))))

(defn bounding-box
  [bounding-box owner]
  (reify
    om/IInitState
    (init-state [_]
      {:width nil
       :height nil
       :just-mounted true})
    om/IDidMount
    (did-mount [_]
      (letfn [(set-dimensions! [rect]
                (om/set-state! owner :width (object/get rect "width"))
                (om/set-state! owner :height (object/get rect "height"))
                (.requestAnimationFrame js/window
                                        (fn [] (om/set-state! owner :just-mounted false))))
              (image-loaded? [rect]
                (and (> (object/get rect "width") 20) (> (object/get rect "height") 20)))]
        (let [rect (.getBoundingClientRect (.getElementById js/document "camelot-preview-image"))]
          (if (image-loaded? rect)
            (set-dimensions! rect)
            (.addEventListener (.getElementById js/document "camelot-preview-image") "load"
                               (fn [evt]
                                 (set-dimensions! (.getBoundingClientRect (.-target evt)))))))))
    om/IRenderState
    (render-state [_ state]
      (when (and (:width state) (:height state))
        (let [area (* (:width bounding-box) (:height bounding-box))
              base-styles {:left (str (* (:min-x bounding-box) (:width state)) "px")
                           :top (str (* (:min-y bounding-box) (:height state)) "px")
                           :width (str (* (:width bounding-box) (:width state)) "px")
                           :height (str (* (:height bounding-box) (:height state)) "px")
                           :zIndex (int (- 20 (* area 10)))}]
          (dom/button #js {:className "bounding-box"
                           :title (:label bounding-box)
                           :style (clj->js (if (:just-mounted state)
                                             (assoc base-styles :opacity "0.7")
                                             base-styles))
                           :onClick #(do (om/transact! (state/library-state) :show-identification-panel not)
                                         (om/update! (state/library-state) :identify-single-image
                                                     (:selected-media-id (state/library-state)))
                                         (om/update! (state/library-state) :identification-bounding-box
                                                     (dissoc bounding-box :label))
                                         (.preventDefault %)
                                         (.stopPropagation %))}))))))

(defn suggestion-bounding-box
  [suggestion owner]
  (reify
    om/IRender
    (render [_]
      (if-let [bb (:bounding-box suggestion)]
        (om/build bounding-box (assoc bb :label "Unidentified"))))))

(defn sighting-bounding-box
  [sighting owner]
  (reify
    om/IRender
    (render [_]
      (if-let [bb (:bounding-box sighting)]
        (om/build bounding-box (assoc bb :label
                                      (str
                                       (:sighting-quantity sighting) "x "
                                       (or (:taxonomy-label (get (:species (state/library-state))
                                                                 (:taxonomy-id sighting)))
                                           (tr/translate ::species-not-in-survey)))))))))

(defn mcp-preview
  [selected owner]
  (reify
    om/IInitState
    (init-state [_]
      {:brightness 0
       :contrast 0})
    om/IDidUpdate
    (did-update [_ {:keys [media-uri]} {:keys [brightness contrast]}]
      (when (and (not= media-uri (get selected :media-uri))
                 (or (not (zero? brightness)) (not (zero? contrast))))
        (om/set-state! owner :brightness 0)
        (om/set-state! owner :contrast 0)))
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "preview"}
               (if selected
                 (dom/a #js {:href (str (get selected :media-uri))
                             :target "_blank"
                             :rel "noopener noreferrer"}
                        (dom/div nil
                                 (om/build-all suggestion-bounding-box (:suggestions selected)
                                               {:key :suggestion-id})
                                 (om/build-all sighting-bounding-box (:sightings selected)
                                               {:key :sighting-id})
                                 (dom/img #js {:id "camelot-preview-image"
                                               :src (str (get selected :media-uri))
                                               :style #js {:filter (str "brightness(" (exp-perc (:brightness state)) ") "
                                                                        "contrast(" (exp-perc (:contrast state)) ")")
                                                           :backgroundImage (str "url("(get selected :media-uri) "/thumb)")
                                                           :backgroundSize "cover"}})
                                 (om/build preview-adjustment-panel
                                           {:brightness (:brightness state)
                                            :update-brightness!
                                            #(om/set-state! owner :brightness (.. % -target -value))

                                            :contrast (:contrast state)
                                            :update-contrast!
                                            #(om/set-state! owner :contrast (.. % -target -value))})))
                 (dom/div #js {:className "none-selected"}
                          (dom/h4 nil photo-not-selected)))))))

(defn display-sighting-field-details
  [{:keys [field sighting]} owner]
  (reify
    om/IRender
    (render [_]
      (let [value (get sighting (util.sf/user-key field))]
        (when-not (or (nil? value) (= value ""))
          (dom/div nil (:sighting-field-label field) ": " (str value)))))))

(defn load-sighting-details
  [data survey-id sighting]
  (om/update! data :identification
              {:quantity (:sighting-quantity sighting)
               :species (str (:taxonomy-id sighting))
               :sighting-id (:sighting-id sighting)
               :sighting-fields (into {}
                                      (mapv #(let [user-key (util.sf/user-key %)]
                                               (vector (:sighting-field-id %)
                                                       (get sighting user-key)))
                                            (util/survey-sighting-fields survey-id)))}))

(defn mcp-details-sightings
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [sighting (:sighting data)]
        (dom/div #js {:className "data"}
                 (if (> (:sighting-id sighting) -1)
                   (dom/div #js {:className "fa fa-trash remove-sighting"
                                 :onClick #(do
                                             (remove-sighting (state/library-state) (:sighting-id sighting))
                                             (nav/analytics-event "library-preview" "delete-sighting"))}))
                 (dom/a #js {:onClick #(do
                                         (om/update! (:data data) :show-identification-panel true)
                                         (load-sighting-details (:data data) (:survey-id data) sighting))}
                        (:sighting-quantity sighting) "x "
                        (or (:taxonomy-label (get (:species (state/library-state))
                                                  (:taxonomy-id sighting)))
                            (tr/translate ::species-not-in-survey)))
                 (dom/div #js {:className "sighting-extra-details"}
                          (om/build-all display-sighting-field-details
                                        (map #(hash-map :field %
                                                        :sighting sighting)
                                             (sort-by (juxt :sighting-field-ordering :sighting-field-label)
                                                      (util/survey-sighting-fields (:survey-id data))))
                                        {:key-fn #(get-in % [:field :sighting-field-id])})))))))

(defn mcp-detail
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/label nil (:label data))
               (dom/div #js {:className "data"} (or (get (:data data) (:key data)) "-"))))))

(defn mcp-details-breakdown
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [media (:media data)]
        (dom/div #js {:className "details media-details-inner-container"}
                 (om/build-all mcp-detail (map #(merge {:data media} %)
                                               [{:key :trap-station-latitude :label (tr/translate :trap-station/trap-station-latitude.label)}
                                                {:key :trap-station-longitude :label (tr/translate :trap-station/trap-station-longitude.label)}
                                                {:key :trap-station-name :label (tr/translate :trap-station/trap-station-name.label)}
                                                {:key :site-sublocation :label (tr/translate :site/site-sublocation.label)}
                                                {:key :site-name :label (tr/translate :site/site-name.label)}
                                                {:key :camera-name :label (tr/translate :camera/camera-name.label)}])
                               {:key :key})
                 (dom/div nil
                          (dom/label nil (tr/translate :media/media-capture-timestamp.label))
                          (let [df (DateTimeFormat. "HH:mm:ss EEE, dd LLL yyyy")]
                            (dom/div #js {:className "data"}
                                     (if-let [ts (:media-capture-timestamp media)]
                                       (.format df ts)
                                       "-"))))
                 (dom/div nil
                          (when (seq (:sightings media))
                            (dom/div nil
                                     (dom/label nil (tr/translate ::sightings))
                                     (om/build-all mcp-details-sightings (map #(hash-map :survey-id (:survey-id media)
                                                                                         :data (:data data)
                                                                                         :sighting %)
                                                                              (:sightings media))
                                                   {:key-fn #(get-in % [:sighting :sighting-id])})))))))))

(defn mcp-details
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "fa fa-remove pull-right close-details"
                             :onClick #(do (om/transact! data :show-media-details not)
                                           (nav/analytics-event "library-preview" "close-details-click"))})
               (dom/h4 nil (tr/translate :words/details))
               (let [selected (util/find-with-id data (:selected-media-id data))]
                 (if selected
                   (dom/div #js {:className "details-container"}
                            (om/build mcp-details-breakdown {:data data :media selected})
                            (dom/div #js {:className "selection-delete"}
                                     (dom/div #js {:onClick #(om/update! data :show-delete-media-prompt true)}
                                              (dom/span #js {:className "preview-delete-button fa fa-trash"})
                                              (tr/translate ::delete-media))
                                     (dom/div #js {:onClick #(om/update! data :show-delete-sightings-prompt true)}
                                              (dom/span #js {:className "preview-delete-button fa fa-trash"})
                                              (tr/translate ::delete-sightings))))
                   (dom/div #js {:className "media-details-inner-container"}
                            photo-not-selected)))))))

(defn details-panel-class
  [data base]
  (str base (if (:show-media-details data)
              " show-panel"
              "")))

(defn toggle-details-panel
  [data]
  (om/transact! data :show-media-details not))

(defn media-details-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className (details-panel-class data "media-details-panel")}
                        (dom/div #js {:id "details-panel-toggle"
                                      :className "details-panel-toggle"
                                      :onClick #(do (toggle-details-panel data)
                                                    (nav/analytics-event "library-preview" "details-toggle-click"))}))
               (dom/div #js {:className (details-panel-class data "media-details-panel-text")}
                        (dom/div #js {:className "details-panel-toggle-text"
                                      :onClick #(do (toggle-details-panel data)
                                                    (nav/analytics-event "library-preview" "details-toggle-click"))}
                                 (dom/div #js {:className "rotate"} (tr/translate :words/details)))
                        (om/build mcp-details data))))))

(defn media-control-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [media (util/find-with-id data (:selected-media-id data))]
        (dom/div #js {:className "media-control-panel"}
                 (om/build media-details-panel-component data)
                 (dom/div #js {:className "mcp-container"}
                          (om/build mcp-preview media)))))))
