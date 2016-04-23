(ns camelot.smithy.core)

(defmacro defsmith
  [smith-name smiths-atom vars mould]
  {:pre [(coll? vars)
         (= (count vars) 1)]}
  `(swap! ~smiths-atom assoc ~(keyword smith-name)
          (fn [~(first vars)]
            ~mould)))

(defsmith survey smiths
  [params]
  {:thing params})

(defn describe-schema
  "Add label and description data to the given schema definition"
  [rtype schema translate-fn]
  (reduce (fn [acc [k v]]
            (assoc acc k
                   {:label (translate-fn rtype (keyword (format "%s/label" (name k))))
                    :description (translate-fn rtype (keyword (format "%s/description" (name k))))
                    :schema v})) {} schema))

(defn build-smith-reducer
  [translate-fn params]
  (fn [acc k v]
    (let [ps (v params)]
      (assoc acc k (assoc ps :schema (describe-schema (:type (:resource ps)) (:schema ps)
                                                      translate-fn))))))

(defn build-smiths
  [smiths-atom translate-fn params]
  (reduce-kv (build-smith-reducer translate-fn params) {} @smiths-atom))
