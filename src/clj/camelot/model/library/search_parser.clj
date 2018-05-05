(ns camelot.model.library.search-parser
  (:require
   [clojure.edn :as edn]
   [camelot.util.model :as model]
   [camelot.util.search :as search-util]
   [clojure.string :as str]))

(defmacro with-negation [search [s-bind is-neg?] & body]
  (let [s# search]
    `(let [found-bang# (= (first ~s#) \!)
           ~is-neg? found-bang#
           ~s-bind (if found-bang# (subs ~s# 1) ~s#)]
       ~@body)))

(defn sighting-field?
  [field]
  (re-find #"field-" (name field)))

(defn parse-term
  [search]
  (with-negation search [s is-neg?]
    (let [parts (str/split s #":")]
      (if (= (count parts) 1)
        {:value (first parts)
         :negated? is-neg?}
        (let [field (search-util/field-key-lookup (keyword (first parts)))]
          {:field field
           :sighting-field? (sighting-field? field)
           :table (:table (get model/schema-definitions field))
           :id-field? (some? (re-find #"\-id$" (name field)))
           :value (str/join ":" (rest parts))
           :negated? is-neg?})))))

(defn parse-conjunction
  [search]
  (reverse (sort-by (juxt :id-field? :field) (map parse-term (str/split search #"\+\+\+")))))

(defn parse-disjunctions
  [search]
  (map parse-conjunction (str/split search #"\|")))

(defn format-reducer
  [acc c]
  (cond
    (and (= c \ ) (not (:quoted acc)))
    (if (:is-conj acc)
      acc
      (update (assoc acc :is-conj true) :result #(conj % "+++")))

    (= c \")
    (update (assoc acc :is-conj false) :quoted not)

    :else
    (update (assoc acc :is-conj false) :result #(conj % c))))

(defn format-terms
  [terms]
  (->> terms
       str/trim
       seq
       (reduce format-reducer {:result [] :quoted false})
       :result
       (apply str)
       str/lower-case
       str/trim))

(defn parse
  [search]
  (let [t (format-terms (or search ""))]
    (if (= t "")
      []
      (parse-disjunctions t))))

(defn has-disjunctions?
  [psearch]
  (> (count psearch) 1))

(defn match-all?
  [psearch]
  (= (count psearch) 0))

(defn match-all-in-survey?
  [psearch]
  (and (= (count psearch) 1)
       (= (count (first psearch)) 1)
       (= (:field (ffirst psearch)) :survey-id)))
