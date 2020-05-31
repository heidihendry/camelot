(ns camelot.state.util
  (:require [clojure.java.io :as io]))

;; TODO #217 apply this to datasets too
(defn paths-to-file-objects
  "Transform all values under :paths to `File` objects."
  [m]
  (letfn [(to-file-objects [p] (into {} (map (fn [[k v]] [k (io/file v)]) p)))]
    (update m :paths to-file-objects)))
