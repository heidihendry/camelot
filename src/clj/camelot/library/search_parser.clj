(ns camelot.library.search-parser
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

(def operators
  {"<" :lt
   "<=" :le
   ">" :gt
   ">=" :ge
   ":" :eq
   "==" :eq
   "!=" :ne})

(def ^:private operator-pattern
  (str "(<=|<|>=|>|==|:|!=" ")"))

(defmacro with-value-operator [value [v-bind op] & body]
  (let [v# value]
    `(let [pattern# (re-pattern (str "^" operator-pattern))
           found-operator# (re-find pattern# ~v#)
           ~op (get operators (second found-operator#) :eq)
           ~v-bind (if ~op (subs ~v# (count (second found-operator#))) ~v#)]
       ~@body)))

(defn sighting-field?
  [field]
  (re-find #"field-" (name field)))

(defn- xor
  [a b]
  (and (or a b)
       (and (not= a b))))

(defn parse-term
  [search]
  (with-negation search [s is-neg?]
    (let [parts (str/split s (re-pattern operator-pattern))]
      (if (= (count parts) 1)
        {:value (first parts)
         :negated? is-neg?}
        (let [field-and-op (re-pattern (str "^.+?" operator-pattern))
              raw-value (str/replace-first search field-and-op "$1")]
          (with-value-operator raw-value [value op]
            (let [field (search-util/field-key-lookup (keyword (first parts)))]
              {:field field
               :sighting-field? (sighting-field? field)
               :table (:table (get model/schema-definitions field))
               :id-field? (some? (re-find #"\-id$" (name field)))
               :value value
               :operator op
               :negated? (xor is-neg? (= op :ne))})))))))

(defn parse-conjunction
  [search]
  (->> (str/split search #"\+\+\+")
       (map parse-term)
       (sort-by (juxt :id-field? :field))
       reverse
       (remove #(empty? (:value %)))))

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
