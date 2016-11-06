(ns camelot.util.data)

(defn select-keys-inv
  [ks data]
  (select-keys data ks))

(defn- starts-with?
  [prefix s]
  (re-matches (re-pattern (str "^" prefix ".*")) s))

(defn- longest-prefix-match
  [s ps]
  (some->> ps
           (filter #(starts-with? % s))
           (sort-by count >)
           first))

(defn- strip-prefix
  [p s]
  (subs s (count p)))

(defn- strip-key-prefix
  [k p]
  (let [n (apply str (drop-while #(= % \-) (strip-prefix p (name k))))]
    (if (empty? n)
      nil
      (keyword n))))

(defn- prefix-key-reducer-fn
  [ks acc k v]
  (let [m (longest-prefix-match (name k) (map name ks))]
    (if m
      (update acc (keyword m) #(assoc % (strip-key-prefix k m) v))
      (assoc acc k v))))

(defn prefix-key
  [d ks]
  (reduce-kv (partial prefix-key-reducer-fn ks) {} d))

(defn nat?
  "Predicate returning true if n is a natural number (zero incl.)."
  [n]
  (and (number? n)
       (or (zero? n) (pos? n))))
