(ns camelot.model.album
  (:require [schema.core :as s]
            [camelot.model.photo :as mp])
  (:import [camelot.model.photo PhotoMetadata]))

(def RawMetadata {s/Str s/Str})

(def Sightings
  {(s/required-key :species) s/Str
   (s/required-key :count) s/Num})

(def ExtractedMetadata
  {(s/required-key :datetime-start) org.joda.time.DateTime
   (s/required-key :datetime-end) org.joda.time.DateTime
   (s/required-key :make) (s/maybe s/Str)
   (s/required-key :model) (s/maybe s/Str)
   (s/optional-key :sightings) Sightings})

(def Problem
  {(s/required-key :result) s/Keyword
   (s/required-key :reason) s/Str})

(def Album
  {(s/required-key :photos) {java.io.File (s/if mp/valid? PhotoMetadata mp/InvalidPhoto)}
   (s/optional-key :metadata) ExtractedMetadata
   (s/optional-key :invalid) s/Bool
   (s/required-key :problems) [Problem]})
