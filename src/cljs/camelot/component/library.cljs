(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.state :as state]
            [cljs.reader :as reader]
            [clojure.string :as str])
  (:import [goog.i18n DateTimeFormat]))

(def page-size 50)

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
   "attn" :media-attention-needed
   "trapid" :trap-station-id
   "proc" :media-processed
   "city" :site-city})

(defn field-key-lookup
  [f]
  (or (get field-keys f) (keyword f)))

(defn nil->empty
  [v]
  (if (nil? v)
    ""
    v))

(defn substring?
  [s sub]
  (if (not= (.indexOf (str/lower-case (.toString (nil->empty s))) sub) -1)
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
    (some #(substring? (nil->empty (get % (field-key-lookup f))) s) sightings)))

(defn record-string-search
  [search species records]
  (some #(cond
           (= (type %) js/String) (substring? (.toString %) search)
           (= (type %) js/Boolean) (= (.toString %) search))
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
  (if (= search "")
    true
    (disjunctive-terms search species record)))

(defn append-to-strings
  [ss append]
  (map #(str % append) ss))

(defn non-empty-list
  [s]
  (if (empty? s)
    [""]
    s))

(defn append-subfilters
  [s search-conf]
  (-> s
      (str/split #"\|")
      (non-empty-list)
      (append-to-strings (if (:unprocessed-only search-conf) " proc:false" ""))
      (append-to-strings (if (:flagged-only search-conf) " attn:true" ""))
      (append-to-strings (if (and (:trap-station-id search-conf)
                                  (> (:trap-station-id search-conf) -1))
                           (str " trapid:" (:trap-station-id search-conf))
                           ""))
      (#(str/join "|" %))))

(defn only-matching
  [terms data]
  (filter
   #(matches-search? (append-subfilters (str/lower-case (or terms "")) (:search data))
                     (:species data)
                     %)
   (vals (get-in data [:search :results]))))

(defn get-matching
  [data]
  (let [search (:search data)]
    (mapv #(get-in search [:results %]) (:matches search))))

(defn find-with-id
  [media-id]
  (first (filter #(= media-id (:media-id %))
                 (get-matching (state/library-state)))))

(defn hide-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count dec))

(defn all-media-selected
  []
  (filter :selected (get-matching (state/library-state))))

(defn deselect-all
  []
  (dorun (map #(om/update! % :selected false) (all-media-selected))))

(defn load-library-callback
  [resp]
  (om/update! (state/library-state) :selected-media-id nil)
  (om/update! (get (state/library-state) :search) :results
              (reduce-kv (fn [acc k v] (assoc acc k (first v))) {}
                         (group-by :media-id (:body resp))))
  (om/update! (:search (state/library-state)) :page 1)
  (om/update! (:search (state/library-state)) :matches
              (map :media-id (only-matching (get-in (state/library-state) [:search :terms])
                                            (state/library-state)))))

(defn load-library
  ([]
   (rest/get-x "/library" load-library-callback))
  ([survey-id]
   (rest/get-x (str "/library/" survey-id) load-library-callback)))

(defn load-trap-stations
  ([]
   (rest/get-x "/trap-stations"
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp)))))
  ([survey-id]
   (rest/get-x (str "/trap-stations/survey/" survey-id)
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp))))))

(defn add-sighting
  []
  (let [spp (cljs.reader/read-string (get-in (state/library-state) [:identification :species]))
        qty (get-in (state/library-state) [:identification :quantity])
        selected (:selected-media-id (state/library-state))
        all-selected (all-media-selected)]
    (rest/put-x "/library/identify" {:data (merge {:identification
                                                   {:quantity qty
                                                    :species spp}}
                                                  {:media
                                                   (map :media-id all-selected)})}
                (fn [resp]
                  (dorun (map #(do (om/update! (second %)
                                               :sightings
                                               (conj (:sightings (second %))
                                                     {:species-id spp
                                                      :sighting-id (first %)
                                                      :sighting-quantity qty}))
                                   (om/update! (second %) :media-processed true))
                              (zipmap (:body resp) all-selected)))
                  (om/update! (:identification (state/library-state)) :quantity 1)
                  (om/update! (:identification (state/library-state)) :species -1)
                  (om/update! (:search (state/library-state)) :identify-selected false)))))

(defn remove-sighting
  [sighting-id]
  (let [selected (find-with-id (:selected-media-id (state/library-state)))]
    (om/update! selected :sightings
                (filterv (fn [s] (not= sighting-id (:sighting-id s)))
                         (:sightings selected)))
    (rest/delete-resource (str "/sightings/" sighting-id) {} identity)))

(defn identify-selected-prompt
  []
  (om/transact! (:search (state/library-state)) :identify-selected not))

(defn get-media-flags
  [rec]
  (select-keys rec [:media-id
                    :media-attention-needed
                    :media-processed-flag]))

(defn set-flag-state
  [flag-key flag-state]
  (let [selected (all-media-selected)]
    (rest/post-resource "/library/media/flags"
                        {:data (mapv #(assoc (get-media-flags %)
                                             flag-key flag-state)
                                     selected)}
                        (fn []
                          (doall (map
                                  #(om/update! % flag-key flag-state)
                                  selected))))))

(defn set-attention-needed
  [flag-state]
  (set-flag-state :media-attention-needed flag-state))

(defn set-processed
  [flag-state]
  (set-flag-state :media-processed flag-state))

(defn show-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (.setTimeout js/window hide-select-message 1600))

(defn toggle-select-image
  [data ctrl]
  (if (and ctrl (:selected data))
    (om/update! (state/library-state) :selected-media-id nil)
    (om/update! (state/library-state) :selected-media-id (:media-id data)))
  (when (not ctrl)
    (deselect-all))
  (om/transact! data :selected not)
  (show-select-message))

(defn media-on-page
  ([data] (let [data (state/library-state)]
            (vec (take page-size
                       (drop (* (- (get-in data [:search :page]) 1) page-size)
                             (get-matching data))))))
  ([]
   (let [data (state/library-state)]
     (vec (take page-size
                (drop (* (- (get-in data [:search :page]) 1) page-size)
                      (get-matching data)))))))

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
                 (dom/div #js {:className "describe-pagination"}
                           (str (+ (- (* page-size (get-in data [:search :page])) page-size) 1)
                                " - "
                                (min (* page-size (get-in data [:search :page])) matches)
                                " of "
                                matches))
                 (dom/button #js {:className "fa fa-2x fa-angle-right btn btn-default"
                                  :disabled (if (get-in data
                                                        [:search :identify-selected])
                                              "disabled" "")
                                  :onClick #(do (deselect-all)
                                                (om/transact! data [:search :page] (partial next-page matches)))}))))))

(defn trap-station-option-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/option #js {:value (:trap-station-id data)}
                  (:trap-station-name data)))))

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
      (dom/option #js {:value (:species-id data)}
                  (:species-scientific-name data)))))

(defn search-component
  [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (:search data) :page 1)
      (om/update! (:search data) :matches (map :media-id (only-matching nil data)))
      (om/update! (:search data) :terms nil))
    om/IRender
    (render [_]
      (prn "Render")
      (when (-> data :search :dirty-state)
        (prn "Dty")
        (om/update! (:search data) :dirty-state false)
        (om/update! (:search data) :matches
                    (map :media-id (only-matching (-> data :search :terms) data))))
      (let [num-selected (count (all-media-selected))
            selected (find-with-id (:selected-media-id data))]
        (dom/div #js {:className "search-container"}
                 (dom/div #js {:className "search-bar"}
                          (dom/button #js {:className "fa fa-search btn search"
                                           :onClick #(om/update! (:search data) :dirty-state true)})
                          (dom/input #js {:type "text"
                                          :placeholder "Filter..."
                                          :disabled (if (get-in data
                                                                   [:search :identify-selected])
                                                       "disabled" "")
                                          :className "field-input search"
                                          :value (get-in data [:search :terms])
                                          :onChange #(do (om/update! (:search data) :terms (.. % -target -value))
                                                         (om/update! (:search data) :page 1)
                                                         (om/update! (:search data) :dirty-state true)
                                                         )})
                          (dom/span nil " in ")
                          (dom/select #js {:className "survey-select field-input"
                                           :value (:survey-id data)
                                           :onChange #(let [sid (cljs.reader/read-string (.. % -target -value))]
                                                        (if (> sid -1)
                                                          (do
                                                            (load-library sid)
                                                            (load-trap-stations sid))
                                                          (do
                                                            (load-library)
                                                            (load-trap-stations))))}
                                      (om/build-all survey-option-component
                                                    (cons {:survey-id -1 :survey-name "All Surveys"}
                                                          (:surveys data))
                                                    {:key :survey-id}))
                          (om/build pagination-component data)
                          (if (> num-selected 0)
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
                                      "Identify Selected")
                          (let [selected (all-media-selected)]
                            (dom/span nil
                                      (let [flag-enabled (and (seq selected) (every? :media-attention-needed selected))]
                                        (dom/span #js {:className (str "fa fa-2x fa-flag flag"
                                                                       (if flag-enabled
                                                                         " red"
                                                                         ""))
                                                       :title "Attention Needed"
                                                       :onClick #(set-attention-needed (not flag-enabled))}))
                                      (let [flag-enabled (and (seq selected) (every? :media-processed selected))]
                                        (dom/span #js {:className (str "fa fa-2x fa-check processed"
                                                                       (if flag-enabled
                                                                         " green"
                                                                         ""))
                                                       :title "Processed"
                                                       :onClick #(set-processed (not flag-enabled))})))))
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
                                                                                       (get-in data [:identification :species])
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
               (dom/img #js {:className (str "media"
                                             (if (:selected result) " selected" "")
                                             (cond
                                               (:media-attention-needed result) " attention-needed"
                                               (:media-processed result) " processed"
                                               :else ""))
                             :onMouseDown #(toggle-select-image result (.. % -ctrlKey))
                             :src (str (get-in result [:media-uri]) "/thumb")})
               (dom/div #js {:className "view-photo fa fa-eye fa-2x"
                             :onClick #(om/update! (state/library-state) :selected-media-id (:media-id result))})))))


(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (let [matches (get-matching data)]
        (om/update! (:search data) :match-count (count matches))
        (dom/div #js {:className "media-collection"}
                 (dom/div #js {:className "subfilter-bar"}
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
                                                    {:key :trap-station-id})))
                          (dom/div #js {:className "subfilter-option"}
                                   (dom/label #js {} "Unprocessed")
                                   (dom/input #js {:type "checkbox"
                                                   :value (get-in data [:search :unprocessed-only])
                                                   :onChange #(do (om/update! (:search (state/library-state)) :unprocessed-only (.. % -target -checked))
                                                                  (om/update! (:search data) :dirty-state true))
                                                   :className "field-input"}))
                          (dom/div #js {:className "subfilter-option"}
                                   (dom/label #js {} "Flagged")
                                    (dom/input #js {:type "checkbox"
                                                    :value (get-in data [:search :flagged-only])
                                                    :onChange #(do (om/update! (:search (state/library-state)) :flagged-only (.. % -target -checked))
                                                                   (om/update! (:search data) :dirty-state true))
                                                    :className "field-input"})))
                 (when (> (count (all-media-selected)) 1)
                   (dom/div #js {:className (str "selected-count"
                                                 (if (> (get-in data [:search :show-select-count]) 0)
                                                   ""
                                                   " hide-selected"))}
                            (str (count (all-media-selected)) " selected")))
                 (dom/div #js {:className "media-collection-container"}
                          (om/build-all media-component (media-on-page) {:key :media-id})))))))

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
               (let [selected (find-with-id (:selected-media-id data))]
                 (if selected
                   (dom/div #js {:className "details"}
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
                                     (dom/label nil "Site")
                                     (dom/div #js {:className "data"} (:site-name selected)))
                            (dom/div nil
                                     (dom/label nil "Camera")
                                     (dom/div #js {:className "data"} (:camera-name selected)))
                            (dom/div nil
                                     (dom/label nil "Timestamp")
                                     (let [df (DateTimeFormat. "hh:mm:ss EEE, dd LLL yyyy")]
                                       (dom/div nil (.format df (:media-capture-timestamp selected)))))
                            (dom/div nil
                                     (dom/label nil "Sightings")
                                     (om/build-all mcp-details-sightings (:sightings selected)
                                                   {:key :sighting-id}))
                            (dom/div nil
                                     ))
                   (dom/div nil "Photo not selected")))))))

(defn media-control-panel-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "media-control-panel"}
               (dom/div #js {:className "mcp-container"}
                        (om/build mcp-preview (find-with-id (:selected-media-id data))))))))

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
      (om/update! (get-in data [:library :search]) :show-select-count 0)
      (om/update! (get-in data [:library]) :identification {:quantity 1})
      (rest/get-x "/species"
                  (fn [resp]
                    (om/update! (get data :library) :species
                                (into {}
                                      (map #(hash-map (get % :species-id) %)
                                           (:body resp))))))
      (rest/get-x "/surveys"
                  (fn [resp]
                    (om/update! (get data :library) :surveys (:body resp))))

      (load-trap-stations)
      (load-library))
    om/IRender
    (render [_]
      (let [lib (:library data)]
        (if (get-in lib [:search :results])
          (dom/div #js {:className "library"}
                   (om/build search-component lib)
                   (when (get-in lib [:search :matches])
                     (om/build media-collection-component lib))
                   (om/build media-details-panel-component lib)
                   (om/build media-control-panel-component lib))
          (dom/div nil ""))))))
