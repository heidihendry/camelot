(ns camelot.handler.settings
  (:require [camelot.config :as c]
            [camelot.model.settings :as ms]))

(defn- flatten-metadata-structure
  "Transfor metadata structure in to a vector of paths"
  [md]
  (vec (reduce #(concat %1 (if (= (type %2) clojure.lang.Keyword)
                              [[%2]]
                              (mapv (fn [v] [(first %2) v]) (second %2))))
                   []
                   md)))

(def metadata-paths (flatten-metadata-structure ms/metadata-structure))

(defn path-description
  "Return a translation for a given path"
  [state path]
  (let [translate #((:translate state) %)]
    (->> path
         (map name)
         (clojure.string/join ".")
         (str "metadata/")
         (keyword)
         (translate))))

(defn config-description
  "Add label and description data to the given schema definition"
  [state schema]
  (let [translate #((:translate state) %)]
    (reduce (fn [acc [k v]]
              (assoc acc k
                     {:label (translate (keyword (format "config/%s/label" (name k))))
                      :description (translate (keyword (format "config/%s/description" (name k))))
                      :schema v})) {} schema)))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (path-description state %)) metadata-paths)))

(defn translate-menu-labels
  "Return a menu with its labels translated"
  [state menu]
  (vec (map #(if (= (first %) :label)
               [(first %) ((:translate state) (second %))]
               %) menu)))

(defn settings-schema
  "Return settings, menu and configuration definitions"
  [state]
  {:config (config-description state (ms/config-schema state))
   :metadata (get-metadata state)
   :menu (translate-menu-labels state ms/config-menu)})

(defn settings-save
  "Save a configuration."
  [config]
  (c/save-config config))
