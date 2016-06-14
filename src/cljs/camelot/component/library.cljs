(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [clojure.string :as str])
  (:import [goog.i18n DateTimeFormat]))

(def page-size 50)

(defn hide-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count dec))

(defn show-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (.setTimeout js/window hide-select-message 1600))

(defn toggle-select-image
  [data]
  (show-select-message)
  (om/update! data :selected (not (:selected data)))
  (om/update! (state/library-state) :selected data))

(defn media-on-page
  []
  (let [data (state/library-state)]
    (take page-size
          (drop (* (- (get-in data [:search :page]) 1)
                   page-size)
                (get-in data [:search :matches])))))

(defn media-selected
  []
  (filter identity (map :selected (get-in (state/library-state) [:search :matches]))))

(defn select-all
  []
  (dorun (map #(om/update! % :selected true) (media-on-page))))

(defn select-all*
  []
  (show-select-message)
  (select-all))

(defn deselect-all
  []
  (dorun (map #(om/update! % :selected false)
              (get-in (state/library-state) [:search :matches]))))

(defn deselect-all*
  []
  (show-select-message)
  (deselect-all))

(defn prev-page
  [page]
  (if (= page 1)
    page
    (do
      (dec page))))

(defn next-page
  [matches page]
  (if (= (.ceil js/Math (/ matches page-size)) page)
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
                                  :onClick #(do (deselect-all)
                                              (om/transact! data [:search :page] prev-page))})
                 (dom/span #js {:className "describe-pagination"}
                           (str (+ (- (* page-size (get-in data [:search :page])) page-size) 1)
                                " - "
                                (min (* page-size (get-in data [:search :page]))
                                     matches)
                                " of "
                                matches))
                 (dom/button #js {:className "fa fa-2x fa-angle-right btn btn-default"
                                  :onClick #(do (deselect-all)
                                                (om/transact! data [:search :page] (partial next-page matches)))}))))))

(defn search-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "search-bar"}
               (om/build pagination-component data)
               (dom/span #js {:className "fa fa-search"})
               (dom/input #js {:type "text"
                               :placeholder "Filter..."
                               :className "field-input search"
                               :value (get-in data [:search :terms])
                               :onChange #(om/update! (:search data)
                                                      :terms
                                                      (.. % -target -value))})
               (if (= page-size (count (media-selected)))
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick deselect-all*}
                             "Select None")
                 (dom/button #js {:className "btn btn-primary"
                                  :onClick select-all*}
                             "Select All"))))))

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
                             :onMouseDown #(toggle-select-image result)
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
  (filterv
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
      (let [matches (only-matching data)]
        (om/update! (:search data) :matches matches)
        (when (not= (get-in data [:search :match-count]) (count matches))
          (om/update! (:search data) :match-count (count matches)))
        (dom/div #js {:className "media-collection"}
                 (om/build search-component data)
                 (dom/div #js {:className (str "selected-count"
                                               (if (> (get-in data [:search :show-select-count]) 0)
                                                 ""
                                                 " hide-selected"))}
                          (str (count (media-selected)) " selected"))
                 (dom/div #js {:className "media-collection-container"}
                          (om/build-all media-component (media-on-page)
                                        {:key :media-id})))))))

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
      (om/update! (get-in data [:library :search]) :page 1)
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
               (om/build media-control-panel-component (:library data))
               (om/build media-collection-component (:library data))))))
