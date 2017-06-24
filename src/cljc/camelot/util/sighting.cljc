(ns camelot.util.sighting
  (:require [clojure.string :as str]))

(defn unidentified?
  "Predicate for whether a value could be considered unidentified."
  [v]
  (or (nil? v)
      (and (string? v) (or (empty? v)
                           (= (str/lower-case v) "unidentified")))))
