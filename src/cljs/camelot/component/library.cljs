(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [cljs.reader :as reader]
            [clojure.string :as str])
  (:import [goog.i18n DateTimeFormat]))

(def page-size 50)

(defn hide-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count dec))

(defn deselect-all
  []
  (dorun (map #(om/update! % :selected false)
              (get-in (state/library-state) [:search :matches]))))

(defn media-on-page
  []
  (let [data (state/library-state)]
    (take page-size
          (drop (* (- (get-in data [:search :page]) 1)
                   page-size)
                (get-in data [:search :matches])))))

(defn media-selected
  []
  (filterv :selected (get-in (state/library-state) [:search :matches])))

(defn add-sighting
  []
  (let [spp (cljs.reader/read-string (get-in (state/library-state) [:identification :species]))
        qty (get-in (state/library-state) [:identification :quantity])
        selected (:selected (state/library-state))]
    (let [all-selected (media-selected)]
      (dorun (map #(om/update! %
                               :sightings
                               (conj (:sightings %)
                                     {:species-id spp
                                      :sighting-id -1
                                      :sighting-quantity qty}))
                  all-selected))
      (rest/put-x "/library/identify" {:data (merge {:identification
                                                     {:quantity qty
                                                      :species spp}}
                                                    {:media
                                                     (map :media-id (media-selected))})}
                  (fn [resp]
                    (dorun (map #(om/update! (second %)
                                             :sightings
                                             (conj (:sightings (second %))
                                                   {:species-id spp
                                                    :sighting-id (first %)
                                                    :sighting-quantity qty}))
                                (zipmap (:body resp) all-selected)))
                    (om/update! (state/library-state) :selected
                                (first (filter #(= (:media-id selected) (:media-id %)) (media-on-page))))
                    (om/update! (:identification (state/library-state)) :quantity 1)
                    (om/update! (:identification (state/library-state)) :species -1)
                    (om/update! (:search (state/library-state)) :identify-selected false)
                    (deselect-all))))))

(defn remove-sighting
  [sighting-id]
  (let [selected (:selected (state/library-state))]
    (om/update! selected :sightings
                (filterv (fn [s] (not= sighting-id (:sighting-id s)))
                         (:sightings selected)))
    (rest/delete-resource (str "/sightings/" sighting-id) {}
                          #(om/update! (state/library-state) :selected
                                       (first (filter (fn [y] (= (:media-id selected) (:media-id y)))
                                                      (media-on-page)))))))

(defn identify-selected-prompt
  []
  (om/transact! (:search (state/library-state)) :identify-selected not))

(defn show-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (.setTimeout js/window hide-select-message 1600))

(defn toggle-select-image
  [data]
  (show-select-message)
  (om/update! data :selected (not (:selected data)))
  (om/update! (state/library-state) :selected data))

(defn select-all
  []
  (dorun (map #(om/update! % :selected true) (media-on-page))))

(defn select-all*
  []
  (show-select-message)
  (select-all))

(defn deselect-all*
  []
  (show-select-message)
  (deselect-all))

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
                                  :disabled (if (get-in data
                                                        [:search :identify-selected])
                                              "disabled" "")
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
                                  :disabled (if (get-in data
                                                        [:search :identify-selected])
                                              "disabled" "")
                                  :onClick #(do (deselect-all)
                                                (om/transact! data [:search :page] (partial next-page matches)))}))))))

(defn species-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:species-id data)}
                  (:species-scientific-name data)))))

(defn search-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [num-selected (count (media-selected))]
        (dom/div #js {:className "search-container"}
                 (dom/div #js {:className "search-bar"}
                          (dom/span #js {:className "fa fa-search"})
                          (dom/input #js {:type "text"
                                          :placeholder "Filter..."
                                          :disabled (if (get-in data
                                                                   [:search :identify-selected])
                                                       "disabled" "")
                                          :className "field-input search"
                                          :value (get-in data [:search :terms])
                                          :onChange #(om/update! (:search data)
                                                                 :terms
                                                                 (.. % -target -value))})
                          (om/build pagination-component data)
                          (if (= page-size num-selected)
                            (dom/button #js {:className "btn btn-default search-main-op"
                                             :onClick deselect-all*
                                             :disabled (if (get-in data
                                                                   [:search :identify-selected])
                                                       "disabled" "")}
                                        "Select None")
                            (dom/button #js {:className "btn btn-default search-main-op"
                                             :disabled (if (get-in data
                                                                   [:search :identify-selected])
                                                       "disabled" "")
                                             :onClick select-all*}
                                        "Select All"))
                          (dom/button #js {:className "btn btn-default"
                                           :onClick identify-selected-prompt
                                           :disabled (if (or (zero? num-selected)
                                                             (get-in data [:search :identify-selected]))
                                                       "disabled" "")}
                                      "Identify Selected"))
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
                                                                            {:species-id -1
                                                                             :species-scientific-name "Select..."})
                                                                      {:key :species-id})))
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
                                                                                       (> (get-in data [:identification :species]) -1)))
                                                                         "disabled")
                                                             :onClick submit-identification} "Submit")
                                            (dom/button #js {:className "btn btn-default"
                                                             :onClick #(om/update! (:search data) :identify-selected false)}
                                                        "Cancel")))))))))

(defn media-component
  "Render a single library item."
  [result owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-item"}
               (dom/img #js {:className (str "media" (if (:selected result) " selected" ""))
                             :onMouseDown #(toggle-select-image result)
                             :src (str (get-in result [:media-uri]) "/thumb")})
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(om/update! (state/library-state) :selected result)})))))


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
                        (dom/img #js {:src (str (get selected :media-uri))}))
                 (dom/div #js {:className "none-selected"}
                          (dom/h4 nil "Photo Not Selected")))))))

(defn mcp-details-sightings
  [sighting owner]
  (reify
    om/IRender
    (render [_]
      (prn (:sighting-id sighting))
      (dom/div nil
               (if (> (:sighting-id sighting) -1)
                 (dom/div #js {:className "fa fa-trash remove-sighting"
                               :onClick #(remove-sighting (:sighting-id sighting))}))
               (:sighting-quantity sighting) "x "
               (:species-scientific-name (get (:species (state/library-state))
                                              (:species-id sighting)))))))

(defn mcp-details
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className "fa fa-remove pull-right close-details"
                             :onClick #(om/transact! data :show-media-details not)})
               (dom/h4 nil "Details")
               (if (:selected data)
                 (dom/div #js {:className "details"}
                          (dom/div nil
                                   (dom/label nil "Longitude")
                                   (dom/div #js {:className "data"} (:trap-station-longitude (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Latitude")
                                   (dom/div #js {:className "data"} (:trap-station-latitude (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Trap Station")
                                   (dom/div #js {:className "data"} (:trap-station-name (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Sublocation")
                                   (dom/div #js {:className "data"} (:site-sublocation (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Site")
                                   (dom/div #js {:className "data"} (:site-name (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Camera")
                                   (dom/div #js {:className "data"} (:camera-name (:selected data))))
                          (dom/div nil
                                   (dom/label nil "Timestamp")
                                   (let [df (DateTimeFormat. "hh:mm:ss EEE, dd LLL yyyy")]
                                     (dom/div nil (.format df (:media-capture-timestamp (:selected data))))))
                          (dom/div nil
                                   (dom/label nil "Sightings")
                                   (om/build-all mcp-details-sightings (:sightings (:selected data))
                                                 {:key :sighting-id})))
                 (dom/div nil "Photo not selected"))))))

(defn media-control-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-control-panel"}
               (dom/div #js {:className "mcp-container"}
                        (om/build mcp-preview (:selected data)))))))

(defn media-details-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/div #js {:className (str "media-details-panel"
                                             (if (:show-media-details data)
                                               " show-panel"
                                               ""))}
                        (dom/div #js {:className "details-panel-toggle"
                                      :onClick #(om/transact! data :show-media-details not)}))
               (dom/div #js {:className (str "media-details-panel-text"
                                             (if (:show-media-details data)
                                               " show-panel"
                                               ""))}
                        (dom/div #js {:className "details-panel-toggle-text"
                                      :onClick #(om/transact! data :show-media-details not)}
                                 (dom/div #js {:className "rotate"}
                                          "Details"))
                        (om/build mcp-details data))))))

(defn library-view-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      ;; TODO For now we assume there's only 1 survey.
      (om/update! (get-in data [:library :search]) :page 1)
      (om/update! (get-in data [:library]) :identification {:quantity 1})
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
               (om/build media-collection-component (:library data))
               (om/build media-details-panel-component (:library data))
               (om/build media-control-panel-component (:library data))))))
