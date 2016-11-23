(ns camelot.handler.application
  "Application-level data request handler."
  (:require
   [camelot.app.screens :as screens]
   [camelot.import.util :as putil]
   [clojure.java.io :as io]
   [camelot.util
    [config :as conf]]
   [ring.util.response :as r]
   [clojure.java.io :as io])
  (:import
   (java.util Properties)))

(defn- version-property-from-pom
  "Return a version string from the Jar metadata."
  [dep]
  (let [path (str "META-INF/maven/" (or (namespace dep) (name dep))
                  "/" (name dep) "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))

(defn- flatten-metadata-structure
  "Transfor metadata structure in to a vector of paths"
  [md]
  (vec (reduce #(into %1 (if (= (type %2) clojure.lang.Keyword)
                              [[%2]]
                              (mapv (fn [v] [(first %2) v]) (second %2))))
                   []
                   md)))

(def metadata-paths (flatten-metadata-structure screens/metadata-structure))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (version-property-from-pom 'camelot)))

(defn get-nav-menu
  "Return the application navigation menu."
  [state]
  (screens/nav-menu state))
