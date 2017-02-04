(ns camelot.util.filter
  (:require
   [clojure.string :as str]
   [camelot.util.model :as model]))

(def field-keys
  {"species" :taxonomy-label
   "genus" :taxonomy-genus
   "family" :taxonomy-family
   "order" :taxonomy-order
   "class" :taxonomy-class
   "common" :taxonomy-common-name
   "site" :site-name
   "camera" :camera-name
   "loc" :site-sublocation
   "trap" :trap-station-name
   "long" :trap-station-longitude
   "lat" :trap-station-latitude
   "model" :camera-model
   "make" :camera-make
   "trapid" :trap-station-id
   "flagged" :media-attention-needed
   "processed" :media-processed
   "testfire" :media-cameracheck
   "reference-quality" :media-reference-quality
   "city" :site-city})

(def model-fields
  (mapv name model/fields))

(defn append-to-strings
  [ss append]
  (map #(str % append) ss))

(defn non-empty-list
  [s]
  (if (empty? s)
    [""]
    s))

(defn append-subfilters
  [s search-conf]
  (-> s
      (str/split #"\|")
      (non-empty-list)
      (append-to-strings (if (:unprocessed-only search-conf) " processed:false" ""))
      (append-to-strings (if (and (:trap-station-id search-conf)
                                  (> (:trap-station-id search-conf) -1))
                           (str " trapid:" (:trap-station-id search-conf))
                           ""))
      (#(str/join "|" %))))
