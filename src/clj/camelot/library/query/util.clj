(ns camelot.library.query.util)

(defn ->column
  [table col]
  (keyword (str (name table) "." (name col))))

(defn filter-nodes
  [pred pt]
  (cond
    (not (coll? pt)) []
    (pred pt) [pt]
    :default (mapcat (partial filter-nodes pred) pt)))

(defn field-names
  [pt]
  (->> pt
       (filter-nodes #(-> % first (= :field)))
       (map second)))

(defn node-values
  [nk node]
  (->> node
       (filter-nodes #(-> % first (= nk)))
       (map second)))

(defn- statements
  [pt]
  (filter-nodes #(-> % first (= :statement)) pt))

(defn match-all?
  [pt]
  (empty? (statements pt)))

(defn- survey-id-field?
  [node]
  (->> node
       (node-values :field)
       first
       keyword
       (= :survey-id)))

(defn first-integer-value
  [node]
  (->> node
       (node-values :integer)
       first))

(defn- concrete-integer-value?
  [node]
  (not= (first-integer-value node) [:wildcard]))

(defn match-all-in-survey?
  [pt]
  (let [n (statements pt)]
    (and (= (count n) 1)
         (and (survey-id-field? (first n))
              (concrete-integer-value? (first n))))))
