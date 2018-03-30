(ns camelot.model.library.filter-parser
  (:require
   [camelot.util.filter :as futil]
   [clojure.string :as str]))

(defmacro with-negation [search [s-bind is-neg?] & body]
  (let [s# search]
    `(let [found-bang# (= (first ~s#) \!)
           ~is-neg? found-bang#
           ~s-bind (if found-bang# (subs ~s# 1) ~s#)]
       ~@body)))

(defn parse-term
  [search]
  (with-negation search [s is-neg?]
    (let [parts (str/split s #":")]
      (if (= (count parts) 1)
        {:value (first parts)
         :negated? is-neg?}
        (let [field (keyword (first parts))]
          {:field field
           :id-field? (re-find #"\-id$" (name (futil/field-key-lookup field)))
           :value (str/join ":" (rest parts))
           :negated? is-neg?})))))

(defn parse-conjunction
  [search]
  (map parse-term (str/split search #"\+\+\+")))

(defn parse-disjunctions
  [search]
  (map parse-conjunction (str/split search #"\|")))

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
