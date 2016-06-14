(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [clojure.string :as str])
  (:import [goog.i18n DateTimeFormat]))

(defn select-image
  [data]
  (om/update! data :selected (not (:selected data)))
  (om/update! (state/library-state) :selected data))

(defn search-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "search-bar"}
               (dom/span #js {:className "fa fa-search"})
               (dom/input #js {:type "text"
                               :placeholder "Filter..."
                               :className "field-input search"
                               :value (get-in data [:search :terms])
                               :onChange #(om/update! (:search data)
                                                      :terms
                                                      (.. % -target -value))})))))

(defn media-component
  "Render a single library item."
  [result owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-item"}
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(om/update! (state/library-state) :selected result)})
               (dom/img #js {:className (str "media" (if (:selected result) " selected" ""))
                             :width "196"
                             :onMouseDown #(select-image result)
                             :src (str (get-in result [:media-uri]) "/thumb")})))))


(def field-keys
  {"species" :species-scientific-name
   "common" :species-common-name
   "site" :site-name
   "camera" :camera-name
   "loc" :site-sublocation
   "trap" :trap-station-name
   "long" :trap-station-longitude
   "lat" :trap-station-latitude
   "model" :camera-model
   "make" :camera-make
   "city" :site-city})

(defn field-key-lookup
  [f]
  (or (get field-keys f) (keyword f)))

(defn substring?
  [s sub]
  (if (not= (.indexOf (str/lower-case (or s "")) sub) -1)
    true
    false))

(defn sighting-record
  [species rec]
  (if (seq (:sightings rec))
    (do
      (map (fn [sighting]
             (let [spp (get species (:species-id sighting))]
               (merge (dissoc (merge rec sighting) :sightings)
                      spp)))
           (:sightings rec)))
    (list rec)))

(defn field-search
  [search species sightings]
  (let [[f s] (str/split search #":")]
    (some #(substring? (get % (field-key-lookup f)) s) sightings)))

(defn record-string-search
  [search species records]
  (some #(when (= (type %) js/String)
           (substring? % search))
        (mapcat vals records)))

(defn record-matches
  [search species record]
  (let [rs (flatten (sighting-record species record))]
    (if (substring? search ":")
      (field-search search species rs)
      (record-string-search search species rs))))

(defn conjunctive-terms
  [search species record]
  (every? #(record-matches % species record) (str/split search #" ")))

(defn disjunctive-terms
  [search species record]
  (some #(conjunctive-terms % species record) (str/split search #"\|")))

(defn matches-search?
  [search species record]
  (if (or (nil? search) (= search ""))
    true
    (disjunctive-terms search species record)))

(defn only-matching
  [data]
  (filter
   #(matches-search? (str/lower-case (or (get-in data [:search :terms]) ""))
                      (get-in data [:species])
                      %)
   (get-in data [:search :results])))

(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-collection"}
               (om/build-all media-component (only-matching data)
                             {:key :media-id})))))

(defn mcp-preview
  [selected owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "preview"}
               (if selected
                 (dom/a #js {:href (str (get selected :media-uri))
                             :target "_blank"}
                        (dom/img #js {:src (str (get selected :media-uri) "/preview")}))
                 (dom/div #js {:className "none-selected"}
                          (dom/h4 nil "Photo Not Selected")))))))

(defn mcp-details-sightings
  [sighting owner]
  (reify
    om/IRender
    (render [_]
      (let [spp (:species (state/library-state))]
        (dom/span nil
                  (:sighting-quantity sighting) "x "
                  (:species-scientific-name (get spp (:species-id sighting))))))))

(defn mcp-details
  [selected owner]
  (reify
    om/IRender
    (render [_]
      (when selected
        (dom/div #js {:className "details"}
                 (dom/div #js {:className "column"}
                          (dom/div nil
                                   (dom/label nil "Longitude")
                                   (dom/div #js {:className "data"} (:trap-station-longitude selected)))
                          (dom/div nil
                                   (dom/label nil "Latitude")
                                   (dom/div #js {:className "data"} (:trap-station-latitude selected)))
                          (dom/div nil
                                   (dom/label nil "Trap Station")
                                   (dom/div #js {:className "data"} (:trap-station-name selected)))
                          (dom/div nil
                                   (dom/label nil "Sublocation")
                                   (dom/div #js {:className "data"} (:site-sublocation selected)))
                          (dom/div nil
                                   (dom/label nil "City")
                                   (dom/div #js {:className "data"} (:site-city selected)))
                          (dom/div nil
                                   (dom/label nil "Site")
                                   (dom/div #js {:className "data"} (:site-name selected))))
                 (dom/div #js {:className "column"}
                          (dom/div nil
                                   (dom/label nil "Timestamp")
                                   (let [df (DateTimeFormat. "hh:mm:ss EEE, dd LLL yyyy")]
                                     (dom/div nil (.format df (:media-capture-timestamp selected)))))
                          (dom/div nil
                                   (dom/label nil "Camera")
                                   (dom/div #js {:className "data"} (:camera-name selected)))
                          (dom/div nil
                                   (dom/label nil "Sightings")
                                   (om/build-all mcp-details-sightings
                                                 (:sightings selected)
                                                 {:key :sighting-id}))))))))

(defn media-control-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-control-panel"}
               (dom/div #js {:className "mcp-container"}
                        (dom/h4 nil "Details")
                        (om/build mcp-preview (:selected data))
                        (om/build mcp-details (:selected data)))))))

(defn library-view-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      ;; TODO For now we assume there's only 1 survey.
      (rest/get-x "/species"
                  (fn [resp] (om/update! (get data :library)
                                         :species
                                         (into {} (map #(hash-map (get % :species-id) %)
                                                       (:body resp))))))
      (rest/get-x "/library"
                  (fn [resp] (om/update! (get-in data [:library :search])
                                         :results (vec (:body resp))))))
    om/IRender
    (render [_]
      (dom/div #js {:className "library"}
               (om/build search-component (:library data))
               (om/build media-control-panel-component (:library data))
               (om/build media-collection-component (:library data))))))
