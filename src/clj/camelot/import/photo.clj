(ns camelot.import.photo
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.model.import :as mi]
            [camelot.import.metadata-utils :as metadata])
  (:import [camelot.model.import ImportPhotoMetadata]))

(s/defn get-time-difference :- s/Num
  "Return the difference between two dates in seconds."
  [orig actual]
  (if (or (nil? orig) (nil? actual))
    0
    (/ (- (tc/to-long actual)
          (tc/to-long orig)) 1000)))

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

(s/defn parse :- (s/if mi/valid-photo? ImportPhotoMetadata mi/ImportInvalidPhoto)
  "Validate a photo's raw metadata and normalise if possible."
  [state
   raw-metadata :- mi/ImportRawMetadata]
  (let [errors (metadata/validate-raw-data state raw-metadata)]
    (if errors
      {:invalid errors}
      (metadata/normalise state raw-metadata))))
