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
  (str/lower-case (apply str (:result (reduce format-reducer {:result []
                                                              :quoted false}
                                              (seq terms))))))

(defn parse
  [search]
  (let [t (format-terms search)]
    (parse-disjunctions t)))

(defn has-disjunctions?
  [psearch]
  (> (count psearch) 1))

(defn match-all?
  [psearch]
  (= (count psearch) 0))
