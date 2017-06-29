(ns camelot.component.library.preview
  "Component for Library media preview with details panel."
  (:require [om.dom :as dom]
            [camelot.state :as state]
            [om.core :as om]
            [camelot.component.library.util :as util]
            [camelot.util.sighting-fields :as util.sf]
            [camelot.util.sighting :as util.sighting]
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

(defn mcp-preview
  [selected owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "preview"}
               (if selected
                 (dom/a #js {:href (str (get selected :media-uri))
                             :target "_blank"
                             :rel "noopener noreferrer"}
                        (dom/img #js {:src (str (get selected :media-uri))}))
                 (dom/div #js {:className "none-selected"}
                          (dom/h4 nil photo-not-selected)))))))

(defn display-sighting-field-details
  [{:keys [field sighting]} owner]
  (reify
    om/IRender
    (render [_]
      (let [value (get sighting (util.sf/user-key field))]
        (when-not (util.sighting/unidentified? value)
          (dom/div nil (:sighting-field-label field) ": " (str value)))))))

(defn mcp-details-sightings
  [sighting owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "data"}
               (if (> (:sighting-id sighting) -1)
                 (dom/div #js {:className "fa fa-trash remove-sighting"
                               :onClick #(do
                                           (remove-sighting (state/library-state) (:sighting-id sighting))
                                           (nav/analytics-event "library-preview" "delete-sighting"))}))
               (:sighting-quantity sighting) "x "
               (or (:taxonomy-label (get (:species (state/library-state))
                                         (:taxonomy-id sighting)))
                   (tr/translate ::species-not-in-survey))
               (let [ls (:sighting-lifestage sighting)
                     sex (:sighting-sex sighting)]
                 (dom/div #js {:className "sighting-extra-details"}
                          (when-not (util.sighting/unidentified? sex)
                            (dom/div nil (tr/translate :sighting/sighting-sex.label) ": " sex))
                          (when-not (util.sighting/unidentified? ls)
                            (dom/div nil (tr/translate :sighting/sighting-lifestage.label) ": " ls))
                          (om/build-all display-sighting-field-details
                                        (map #(hash-map :field %
                                                        :sighting sighting)
                                             (sort-by (juxt :sighting-field-ordering :sighting-field-label)
                                                      (util/survey-sighting-fields (:survey-id sighting)))))))))))

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
      (dom/div #js {:className "details media-details-inner-container"}
               (om/build-all mcp-detail (map #(merge {:data data} %)
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
                                   (if-let [ts (:media-capture-timestamp data)]
                                     (.format df ts)
                                     "-"))))
               (dom/div nil
                        (when (seq (:sightings data))
                          (dom/div nil
                                   (dom/label nil (tr/translate ::sightings))
                                   (om/build-all mcp-details-sightings (map #(assoc % :survey-id (:survey-id data))
                                                                            (:sightings data))
                                                 {:key :sighting-id}))))))))

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
                            (om/build mcp-details-breakdown selected)
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
