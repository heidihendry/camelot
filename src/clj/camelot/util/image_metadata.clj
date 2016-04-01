(ns camelot.util.image-metadata
  (:import [com.drew.metadata Metadata Directory Tag]))

(def getDescription
  #(.getDescription ^Tag %))

(def getTagName
  #(.getTagName ^Tag %))

(def getTags
  #(.getTags ^Directory %))

(def getDirectories
  #(.getDirectories ^Metadata %))
