(ns camelot.handler.settings
  (:require [camelot.processing.settings :as ps]
            [camelot.processing.util :as putil]
            [camelot.model.settings :as ms]))

(defn- flatten-metadata-structure
  "Transfor metadata structure in to a vector of paths"
  [md]
  (vec (reduce #(into %1 (if (= (type %2) clojure.lang.Keyword)
                              [[%2]]
                              (mapv (fn [v] [(first %2) v]) (second %2))))
                   []
                   md)))

(def metadata-paths (flatten-metadata-structure ms/metadata-structure))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))

(defn settings-save
  "Save a configuration."
  [config]
  (ps/save-config config))

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (ps/version-property-from-pom 'camelot)))

(defn get-nav-menu
  [state]
  (ms/nav-menu state))
