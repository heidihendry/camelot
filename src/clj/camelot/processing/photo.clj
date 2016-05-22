(ns camelot.processing.photo
  (:require [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.processing.util :as putil]
            [camelot.model.photo :as mp]
            [camelot.processing.metadata-utils :as metadata]
            [camelot.model.album :as ma])
  (:import [camelot.model.photo PhotoMetadata]))

(s/defn get-time-difference :- s/Num
  "Return the difference between two dates in seconds."
  [orig actual]
  (if (or (nil? orig) (nil? actual))
    0
    (/ (- (tc/to-long actual)
          (tc/to-long orig)) 1000)))

(defn flatten-sightings
  [photo]
  (flatten (mapv #(merge (dissoc photo :sightings) {:sighting %}) (:sightings photo))))

(defn extract-path-value
  "Return the metadata for a given path."
  [metadata path]
  (reduce get metadata path))

(s/defn night? :- s/Bool
  "Check whether the given time is 'night'."
  [night-start night-end hour]
  (or (> hour night-start) (< hour night-end)))

(s/defn infrared-sane? :- s/Bool
  "Check whether the infraresh thresholds for a photo seem valid."
  [nightfn isothresh photo]
  (let [hour (t/hour (:datetime photo))
        iso (:iso (:settings photo))]
    (or (nil? iso) (> iso isothresh) (not (nightfn hour)))))

(s/defn parse :- (s/if mp/valid? PhotoMetadata mp/InvalidPhoto)
  "Validate a photo's raw metadata and normalise if possible."
  [state
   raw-metadata :- ma/RawMetadata]
  (let [errors (metadata/validate-raw-data state raw-metadata)]
    (if errors
      {:invalid errors}
      (metadata/normalise state raw-metadata))))
