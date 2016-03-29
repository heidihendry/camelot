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
   (s/required-key :make) s/Str
   (s/required-key :model) s/Str
   (s/required-key :sightings) Sightings})

(def Album
  {(s/required-key :photos) {java.io.File PhotoMetadata}
   (s/required-key :metadata) ExtractedMetadata
   (s/required-key :problems) [s/Keyword]})
