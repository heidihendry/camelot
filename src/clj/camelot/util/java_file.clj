(ns camelot.util.java-file
  (:import [java.io File]))

(def getParent
  #(.getParent ^File %))

(def getParentFile
  #(.getParentFile ^File %))

(def getName
  #(.getName ^File %))

(def isDirectory
  #(.isDirectory ^File %))

(def toPath
  #(.toPath ^File %))

(def isFile
  #(.isFile ^File %))

(def getPath
  #(.getPath ^File %))

(def exists
  #(.exists ^File %))

(def can-read?
  #(.canRead ^File %))

(def renameTo
  #(.renameTo ^File %1 %2))

(def mkdir
  #(.mkdir ^File %1))

(def pushback-reader
  #(java.io.PushbackReader. ^FileReader %))
