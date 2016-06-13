(ns camelot.component.library
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [clojure.string :as str]))

(defn search-component
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/input #js {:type "text"
                               :placeholder "Search..."
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
      (dom/span nil
                (dom/img #js {:className "media"
                              :width "196"
                              :src (get-in result [:media-uri])})))))


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
               (assoc (dissoc (merge rec sighting) :sightings)
                      :species (:species-scientific-name spp)
                      :common (:species-common-name spp))))
           (:sightings rec)))
    (list rec)))

(defn field-search
  [search species sightings]
  (let [[f s] (str/split search #":")]
    (some #(substring? (get % (keyword f)) s) sightings)))

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

(defn matches-search
  [search species record]
  (if (or (nil? search) (= search ""))
    true
    (disjunctive-terms search species record)))

(defn only-matching
  [data]
  (filter #(matches-search (str/lower-case (or (get-in data [:search :terms]) ""))
                           (get-in data [:species])
                           %)
          (get-in data [:search :results])))

(defn media-collection-component
  "Render a collection of library."
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build-all media-component (only-matching data)
                             {:key :media-id})))))

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
                                         :results (:body resp)))))
    om/IRender
    (render [_]
      (dom/div #js {:className "library"}
               (om/build search-component (:library data))
               (om/build media-collection-component (:library data))))))
