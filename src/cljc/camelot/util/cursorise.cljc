(ns camelot.util.cursorise)

(defn decursorise
  "Remove :value keys used for Om cursors to leaves from the configuration data."
  [conf]
  (if (some :value (vals conf))
    (into {} (map (fn [[k v]] {k (:value v)}) conf))
    conf))

(defn cursorise
  "Add :value keys used for Om cursors to leaves from the configuration data."
  [conf]
  (into {} (map (fn [[k v]] {k {:value v}}) conf)))
