(ns camelot.util.version
  (:require
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

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (version-property-from-pom 'camelot)))
