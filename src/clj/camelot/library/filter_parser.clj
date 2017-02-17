(ns camelot.library.filter-parser
  (:require [clojure.string :as str]))

(defn parse-term
  [search]
  (let [parts (str/split search #":")]
    (if (= (count parts) 1)
      {:value (first parts)}
      {:field (first parts)
       :value (str/join ":" (rest parts))})))

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
       seq
       (reduce format-reducer {:result [] :quoted false})
       :result
       (apply str)
       str/lower-case
       str/trim))

(defn parse
  [search]
  (let [t (format-terms search)]
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
