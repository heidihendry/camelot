(ns camelot.library.filter
  "Record filtering and filter expressions."
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.filter :as futil]
   [camelot.library.filter-parser :as parser])
  (:import
   (java.lang String Boolean)))

(def exact-matches-needed
  #{:sighting-sex :sighting-lifestage})

(defn nil->empty
  [v]
  (if (nil? v)
    ""
    v))

(defn substring?
  [s sub]
  (if (not= (.indexOf (str/lower-case (str (nil->empty s))) sub) -1)
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
  [field]
  (some #(= % field) exact-matches-needed))

(defn field-search
  [search species sightings]
  (let [{f :field s :value} search
        field (futil/field-key-lookup f)]
    (if (re-find #"\-id$" (name field))
      (some? (some #(= (get % field) (edn/read-string s))
                   sightings))
      (some #(if (= s "*")
               (not (nil? (get % field)))
               (if (needs-exact-match? field)
                 (= (str/lower-case (nil->empty (get % field)))
                    (str/lower-case (nil->empty s)))
                 (substring? (nil->empty (get % field)) s)))
            sightings))))

(defn record-string-search
  [search species records]
  (some #(cond
           (= (type %) String) (substring? (str %) (:value search))
           (= (type %) Boolean) (= (str %) (:value search)))
        (mapcat vals records)))

(defn record-matches
  [search species record]
  (let [rs (flatten (sighting-record species record))]
    (if (contains? search :field)
      (field-search search species rs)
      (record-string-search search species rs))))

(defn conjunctive-terms
  [search species record]
  (every? #(record-matches % species record) search))

(defn disjunctive-terms
  [search species record]
  (some #(conjunctive-terms % species record) search))

(defn matches-search?
  [search species record]
  (if (empty? search)
    true
    (disjunctive-terms search species record)))

(defn only-matching
  [terms species records]
  (if (empty? terms)
    records
    (filter #(matches-search? (parser/parse terms) species %) records)))
