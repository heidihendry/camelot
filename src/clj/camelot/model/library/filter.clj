(ns camelot.model.library.filter
  "Record filtering and filter expressions."
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [camelot.util.filter :as futil])
  (:import
   (java.lang String Boolean)))

(defn normalise-str
  [v]
  (if (nil? v)
    ""
    (str/lower-case v)))

(defn substring?
  [s ^String sub]
  (.contains ^String (normalise-str s) sub))

(defn field-search-matches?
  [search record]
  (let [{f :field s :value} search
        field (futil/field-key-lookup f)]
    (if (re-find #"\-id$" (name field))
      (= (get record field) (edn/read-string s))
      (let [val (get record field)]
        (if (= s "*")
          (not (nil? val))
          (substring? (normalise-str val) s))))))

(defn record-string-search-matches?
  [search record]
  (substring? (apply str (interpose "|||" (vals record))) (:value search)))

(defn value-matches?
  [search record]
  (if (contains? search :field)
    (field-search-matches? search record)
    (record-string-search-matches? search record)))

(defn record-matches?
  [search record]
  (if (:negated? search)
    (not (value-matches? search record))
    (value-matches? search record)))

(defn conjunctive-terms
  [search record]
  (every? #(record-matches? % record) search))

(defn disjunctive-terms
  [search record]
  (some #(conjunctive-terms % record) search))

(defn matches-search?
  [search record]
  (if (empty? search)
    true
    (disjunctive-terms search record)))

(defn only-matching
  [psearch records]
  (if (empty? psearch)
    records
    (filter #(matches-search? psearch %) records)))
