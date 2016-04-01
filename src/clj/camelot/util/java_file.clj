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

(def renameTo
  #(.renameTo ^File %1 %2))
