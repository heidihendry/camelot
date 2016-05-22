(ns camelot.model.album
  (:require [schema.core :as s]
            [camelot.model.photo :as mp])
  (:import [camelot.model.photo PhotoMetadata]))

(def RawMetadata {s/Str s/Str})

(def Sightings
  "Sighting counts keyed by species."
  {(s/required-key :species) s/Str
   (s/required-key :count) s/Num})

(def ExtractedMetadata
  "Summary of the Album's contents."
  {(s/required-key :datetime-start) org.joda.time.DateTime
   (s/required-key :datetime-end) org.joda.time.DateTime
   (s/required-key :make) (s/maybe s/Str)
   (s/required-key :model) (s/maybe s/Str)
   (s/optional-key :sightings) Sightings})

(def Problem
  "A single problem with the album."
  {(s/required-key :result) s/Keyword
   (s/required-key :reason) s/Str})

(def Album
  "Album.  If not invalid (:invalid true), it will contain a metadata summary.) "
  {(s/required-key :photos) {java.io.File (s/if mp/valid? PhotoMetadata mp/InvalidPhoto)}
   (s/optional-key :metadata) ExtractedMetadata
   (s/optional-key :invalid) s/Bool
   (s/required-key :problems) [Problem]})
