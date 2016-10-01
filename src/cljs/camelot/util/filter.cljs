(ns camelot.util.filter
  "Record filtering and filter expressions."
  (:require [clojure.string :as str]))

(def field-keys
  {"species" :taxonomy-label
   "genus" :taxonomy-genus
   "family" :taxonomy-family
   "order" :taxonomy-order
   "class" :taxonomy-class
   "common" :taxonomy-common-name
   "site" :site-name
   "camera" :camera-name
   "loc" :site-sublocation
   "trap" :trap-station-name
   "long" :trap-station-longitude
   "lat" :trap-station-latitude
   "model" :camera-model
   "make" :camera-make
   "trapid" :trap-station-id
   "flagged" :media-attention-needed
   "processed" :media-processed
   "testfire" :media-cameracheck
   "reference-quality" :media-reference-quality
   "city" :site-city})

(def model-fields
  ["camera-id"
   "camera-make"
   "camera-model"
   "camera-name"
   "media-attention-needed"
   "media-cameracheck"
   "media-capture-timestamp"
   "media-created"
   "media-filename"
   "media-format"
   "media-id"
   "media-processed"
   "media-updated"
   "media-uri"
   "sighting-id"
   "sighting-created"
   "sighting-quantity"
   "sighting-lifestage"
   "sighting-sex"
   "sighting-updated"
   "site-city"
   "site-id"
   "site-name"
   "site-sublocation"
   "site-state-province"
   "site-country"
   "survey-id"
   "survey-name"
   "survey-site-id"
   "taxonomy-class"
   "taxonomy-created"
   "taxonomy-common-name"
   "taxonomy-family"
   "taxonomy-genus"
   "taxonomy-id"
   "taxonomy-label"
   "taxonomy-notes"
   "taxonomy-order"
   "taxonomy-species"
   "taxonomy-updated"
   "trap-station-id"
   "trap-station-latitude"
   "trap-station-longitude"
   "trap-station-name"
   "trap-station-session-camera-id"
   "trap-station-session-id"])

(def exact-matches-needed
  #{:sighting-sex :sighting-lifestage})

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
             (let [spp (get species (:taxonomy-id sighting))]
               (merge (dissoc (merge rec sighting) :sightings)
                      spp)))
           (:sightings rec)))
    (list rec)))

(defn needs-exact-match?
  [f]
  (some #(= % (field-key-lookup f)) exact-matches-needed))

(defn field-search
  [search species sightings]
  (let [[f s] (str/split search #":")]
    (if (re-find #"\-id$" (name (field-key-lookup f)))
      (some? (some #(= (get % (field-key-lookup f)) (js/parseInt s))
                   sightings))
      (some #(if (= s "*")
               (not (nil? (get % (field-key-lookup f))))
               (if (needs-exact-match? f)
                 (= (str/lower-case (nil->empty (get % (field-key-lookup f))))
                    (str/lower-case (nil->empty s)))
                 (substring? (nil->empty (get % (field-key-lookup f))) s)))
            sightings))))

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
  (every? #(record-matches % species record) (str/split search #"\+\+\+")))

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
      (append-to-strings (if (:unprocessed-only search-conf) " processed:false" ""))
      (append-to-strings (if (and (:trap-station-id search-conf)
                                  (> (:trap-station-id search-conf) -1))
                           (str " trapid:" (:trap-station-id search-conf))
                           ""))
      (#(str/join "|" %))))

(defn format-reducer
  [acc c]
  (cond
    (and (= c " ") (not (:quoted acc)))
    (update acc :result #(conj % "+++"))

    (= c "\"")
    (update acc :quoted not)

    :else
    (update acc :result #(conj % c))))

(defn format-terms
  [terms]
  (str/lower-case (apply str (:result (reduce format-reducer {:result []
                                                              :quoted false}
                                              (seq terms))))))

(defn only-matching
  [terms data species]
  (let [t (format-terms terms)]
    (filter
     #(matches-search? (format-terms (append-subfilters (or t "") data))
                       species
                       %)
     (vals (:results data)))))
