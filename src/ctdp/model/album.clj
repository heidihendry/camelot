(ns ctdp.model.album
  (:require [schema.core :as s]
            [ctdp.model.photo :as mp])
  (:import [ctdp.model.photo PhotoMetadata]))

(def RawMetadata {s/Str s/Str})

(def ExtractedMetadata
  {(s/required-key :datetime-start) org.joda.time.DateTime
   (s/required-key :datetime-end) org.joda.time.DateTime
   (s/required-key :make) s/Str
   (s/required-key :model) s/Str})

(def Album
  {(s/required-key :photos) {java.io.File PhotoMetadata}
   (s/required-key :metadata) ExtractedMetadata
   (s/required-key :problems) [s/Keyword]})
