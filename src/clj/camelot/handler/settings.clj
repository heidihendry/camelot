(ns camelot.handler.settings
  (:require [camelot.processing.settings :as ps]
            [camelot.processing.util :as putil]
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
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))

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
  (ps/save-config config))

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (-> (ps/version-property-from-pom 'camelot))))

(defn get-nav-menu
  [state]
  (ms/nav-menu state))
