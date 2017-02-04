(ns camelot.library.filter
  "Record filtering and filter expressions."
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.filter :as futil])
  (:import
   (java.lang String Boolean)))

(def exact-matches-needed
  #{:sighting-sex :sighting-lifestage})

(defn field-key-lookup
  [f]
  (or (get futil/field-keys f) (keyword f)))

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
  [f]
  (some #(= % (field-key-lookup f)) exact-matches-needed))

(defn field-search
  [search species sightings]
  (let [[f s] (str/split search #":")]
    (if (re-find #"\-id$" (name (field-key-lookup f)))
      (some? (some #(= (get % (field-key-lookup f)) (edn/read-string s))
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
           (= (type %) String) (substring? (str %) search)
           (= (type %) Boolean) (= (str %) search))
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

(defn format-reducer
  [acc c]
  (cond
    (and (= c \ ) (not (:quoted acc)))
    (update acc :result #(conj % "+++"))

    (= c \")
    (update acc :quoted not)

    :else
    (update acc :result #(conj % c))))

(defn format-terms
  [terms]
  (str/lower-case (apply str (:result (reduce format-reducer {:result []
                                                              :quoted false}
                                              (seq terms))))))

(defn only-matching
  [terms species records]
  (if (empty? terms)
    records
    (let [t (format-terms terms)]
      (filter #(matches-search? t species %) records))))
