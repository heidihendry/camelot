(ns camelot.util.data
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
