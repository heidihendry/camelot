(ns camelot.component.library.preview
  "Component for Library media preview with details panel."
  (:require [om.dom :as dom]
            [camelot.state :as state]
            [om.core :as om]
            [camelot.component.library.util :as util]
            [camelot.rest :as rest]
            [camelot.nav :as nav])
  (:import [goog.i18n DateTimeFormat]))

(def photo-not-selected "Photo not selected")

(defn remove-sighting
  [sighting-id]
  (let [selected (util/find-with-id (:selected-media-id (state/library-state)))]
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
                             :target "_blank"}
                        (dom/img #js {:src (str (get selected :media-uri))}))
                 (dom/div #js {:className "none-selected"}
                          (dom/h4 nil photo-not-selected)))))))

(defn mcp-details-sightings
  [sighting owner]
  (reify
    om/IRender
    (render [_]
      (dom/div {:className "data"}
               (if (> (:sighting-id sighting) -1)
                 (dom/div #js {:className "fa fa-trash remove-sighting"
                               :onClick #(do
                                           (remove-sighting (:sighting-id sighting))
                                           (nav/analytics-event "library-preview" "delete-sighting"))}))
               (:sighting-quantity sighting) "x "
               (:taxonomy-label (get (:species (state/library-state))
                                       (:taxonomy-id sighting)))))))

(defn mcp-detail
  [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
               (dom/label nil (:label state))
               (dom/div #js {:className "data"} (get data (:key state)))))))

(defn mcp-details-breakdown
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "details"}
               (map #(om/build mcp-detail data {:init-state %})
                    [{:key :trap-station-latitude :label "Latitude"}
                     {:key :trap-station-longitude :label "Longitude"}
                     {:key :trap-station-name :label "Trap Station"}
                     {:key :site-sublocation :label "Sublocation"}
                     {:key :site-name :label "Site"}
                     {:key :camera-name :label "Camera"}])
               (dom/div nil
                        (dom/label nil "Timestamp")
                        (let [df (DateTimeFormat. "hh:mm:ss EEE, dd LLL yyyy")]
                          (dom/div {:className "data"}
                                   (.format df (:media-capture-timestamp data)))))
               (dom/div nil
                        (dom/label nil "Sightings")
                        (om/build-all mcp-details-sightings (:sightings data)
                                      {:key :sighting-id}))))))

(defn mcp-details
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "fa fa-remove pull-right close-details"
                             :onClick #(do (om/transact! data :show-media-details not)
                                           (nav/analytics-event "library-preview" "close-details-click"))})
               (dom/h4 nil "Details")
               (let [selected (util/find-with-id (:selected-media-id data))]
                 (if selected
                   (om/build mcp-details-breakdown selected)
                   (dom/div nil photo-not-selected)))))))

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
                                 (dom/div #js {:className "rotate"} "Details"))
                        (om/build mcp-details data))))))

(defn media-control-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [media (util/find-with-id (:selected-media-id data))]
        (dom/div #js {:className "media-control-panel"}
                 (om/build media-details-panel-component data)
                 (dom/div #js {:className "mcp-container"}
                          (om/build mcp-preview media)))))))
