(ns camelot.util.search
  (:require
   [clojure.string :as str]
   [camelot.util.model :as model]))

(def field-keys
  {:species :taxonomy-label
   :genus :taxonomy-genus
   :family :taxonomy-family
   :order :taxonomy-order
   :class :taxonomy-class
   :common :taxonomy-common-name
   :site :site-name
   :session-start :trap-station-session-start-date
   :session-end :trap-station-session-start-end
   :captured :media-capture-timestamp
   :camera :camera-name
   :loc :site-sublocation
   :trap :trap-station-name
   :long :trap-station-longitude
   :lat :trap-station-latitude
   :model :camera-model
   :make :camera-make
   :flagged :media-attention-needed
   :processed :media-processed
   :testfire :media-cameracheck
   :reference-quality :media-reference-quality
   :city :site-city})

(defn field-key-lookup
  [f]
  (or (get field-keys f) f))

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

(defn valid-id?
  [id?]
  (and id? (> id? -1)))

(defn append-subfilters
  [s search-conf]
  (-> s
      (str/split #"\|")
      (non-empty-list)
      (append-to-strings (if (:unprocessed-only search-conf) " processed:false" ""))
      (append-to-strings (if (:animals-only search-conf) " (suggestion-key:animal or sighting-id:*)" ""))
      (append-to-strings (if (valid-id? (:trap-station-id search-conf))
                           (str " trap-station-id:" (:trap-station-id search-conf))
                           ""))
      (append-to-strings (if (valid-id? (:survey-id search-conf))
                           (str " survey-id:" (:survey-id search-conf))
                           ""))
      (#(str/join "|" %))))
