(ns ctdp.model.album
  (:require [schema.core :as s])
  (:import [ctdp.model.photo PhotoMetadata]))

(def RawMetadata {s/Str s/Str})

(def Album
  {(s/required-key :photos) {java.io.File PhotoMetadata}
   (s/required-key :problems) [s/Keyword]})
