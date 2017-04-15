(ns camelot.util.data
  "Generic data transformation utils."
  (:require
   [clojure.string :as str]))

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

(defn- map-keys-reducer-fn
  [ks acc k v]
  (if (and (contains? (set ks) k) (map? v))
    (merge acc (reduce-kv
                #(assoc %1 (keyword (str (name k) "-" (name %2))) %3)
                {} v))
    (assoc acc k v)))

(defn map-keys-to-key-prefix
  [d ks]
  (reduce-kv (partial map-keys-reducer-fn ks) {} d))

(defn- strip-prefix
  [p s]
  (subs s (count p)))

(defn- strip-key-prefix
  [k p]
  (let [n (str/join (drop-while #(= % \-) (strip-prefix p (name k))))]
    (when (seq n)
      (keyword n))))

(defn- key-prefix-reducer-fn
  [ks acc k v]
  (let [m (longest-prefix-match (name k) (map name ks))]
    (if m
      (update acc (keyword m) #(assoc % (strip-key-prefix k m) v))
      (assoc acc k v))))

(defn key-prefix-to-map
  [d ks]
  (reduce-kv (partial key-prefix-reducer-fn ks) {} d))

(defn nat?
  "Predicate returning true if n is a natural number (zero incl.)."
  [n]
  (and (number? n)
       (or (zero? n) (pos? n))))

(defn pair?
  "Predicate returning true if x is a coll consisting representing a key and a
  value. False otherwise."
  [x]
  (let [c (count x)]
    (or (and (coll? x) (not (map? x)) (= c 2)) (and (map? x) (= c 1)))))

(defn map-val
  "Map applying f, a function taking two arguments: a key and a value. Return
  the result as a hash-map."
  [f xs]
  {:pre [(ifn? f)]}
  (if (not (or (nil? xs) (coll? xs)))
    (throw (IllegalArgumentException. (str "coll expected, but '" xs "' is not a coll")))
    (into {} (map (fn [[k v]] [k (f v)]) xs))))

(defn key-by
  "Key `xs` by the result of applying `f` to each item.
  Should multiple items in xs return the same value of f, yield only the
  first."
  [f xs]
  {:pre [(ifn? f)]}
  (if (not (or (nil? xs) (coll? xs)))
    (throw (IllegalArgumentException. (str "coll expected, but '" xs "' is not a coll")))
    (->> xs
         (group-by f)
         (map-val (fn [v] (first v))))))
