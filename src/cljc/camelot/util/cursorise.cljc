(ns camelot.util.cursorise
  "Transform map leaves to and from Om-cursor-friendly' structures.")

(defn decursorise
  "Remove :value keys used for Om cursors to leaves from the configuration data."
  [node]
  (if (map? node)
    (into {} (map (fn [[k v]] {k (if (and (map? v) (contains? v :value))
                                   (decursorise (:value v))
                                   (decursorise v))})
                  node))
    node))

(defn cursorise
  "Add :value keys used for Om cursors to leaves from the configuration data."
  [node]
  (if (map? node)
    (into {} (map (fn [[k v]] (if (= k :value)
                                {k v}
                                {k (cursorise v)}))
                  node))
    {:value node}))
