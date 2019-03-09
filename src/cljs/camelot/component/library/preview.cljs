(ns camelot.component.library.preview
  "Component for Library media preview with details panel."
  (:require [om.dom :as dom]
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

(defn preview-adjustment-slider
  [{:keys [label value update!]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "slider"}
               (dom/label nil label)
               (dom/input #js {:type "range"
                               :min -2.5
                               :max 2.5
                               :step 0.005
                               :value value
                               :onClick #(.preventDefault %)
                               :onChange update!})))))

(defn preview-adjustment-panel
  [{:keys [brightness update-brightness! contrast update-contrast!]} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "preview-adjustment-panel"}
               (dom/div #js {:className "panel-background"})
               (dom/div #js {:className "input-container"}
                        (om/build preview-adjustment-slider
                                  {:label (tr/translate ::brightness-label)
                                   :value brightness
                                   :update! update-brightness!})
                        (om/build preview-adjustment-slider
                                  {:label (tr/translate ::contrast-label)
                                   :value contrast
                                   :update! update-contrast!}))))))

(defn mcp-preview
  [selected owner]
  (reify
    om/IInitState
    (init-state [_]
      {:brightness 0
       :contrast 0})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "preview"}
               (if selected
                 (dom/a #js {:href (str (get selected :media-uri))
                             :target "_blank"
                             :rel "noopener noreferrer"}
                        (dom/div nil
                                 (dom/img #js {:src (str (get selected :media-uri))
                                               :style #js {:filter (str "brightness(" (.exp js/Math (:brightness state)) ") contrast(" (.exp js/Math (:contrast state)) ")")}})
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
                          (om/build mcp-preview media
                                    {:react-key (:selected-media-id data)})))))))
