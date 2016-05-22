(ns camelot.util.application
  (:require [clojure.java.io :as io])
  (:import [java.util Properties]))

(defn gen-state
  "Return the global application state.
Currently the only application state is the user's configuration."
  [conf]
  {:config conf})

(defn version-property-from-pom
  "Return a version string from the Jar metadata."
  [dep]
  (let [path (str "META-INF/maven/" (or (namespace dep) (name dep))
                  "/" (name dep) "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))
