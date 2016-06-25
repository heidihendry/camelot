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
   "flag" :media-attention-needed
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
             (let [spp (get species (:taxonomy-id sighting))]
               (merge (dissoc (merge rec sighting) :sightings)
                      spp)))
           (:sightings rec)))
    (list rec)))

(defn field-search
  [search species sightings]
  (let [[f s] (str/split search #":")]
    (if (re-find #"\-id$" (name (field-key-lookup f)))
      (some? (some #(= (get % (field-key-lookup f)) (js/parseInt s))
                   sightings))
      (some #(if (= s "*")
               (not (nil? (get % (field-key-lookup f))))
               (substring? (nil->empty (get % (field-key-lookup f))) s))
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
      (append-to-strings (if (:flagged-only search-conf) " flag:true" ""))
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