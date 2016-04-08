(ns camelot.util.java-file
  (:import [java.io File]))

(def get-parent
  "Return the parent as a path."
  #(.getParent ^File %))

(def get-parent-file
  "Return the parent as a file."
  #(.getParentFile ^File %))

(def get-name
  "Return the name of the given file."
  #(.getName ^File %))

(def directory?
  "Predicate for whether the file is a directory."
  #(.isDirectory ^File %))

(def to-path
  #(.toPath ^File %))

(def file?
  "Predicate for whether the File object is a file."
  #(.isFile ^File %))

(def get-path
  "Return the path to the given file as a String."
  #(.getPath ^File %))

(def exists?
  "Predicate for whether the file exists"
  #(.exists ^File %))

(def readable?
  "Predicate for whether the given file is readable."
  #(.canRead ^File %))

(def rename-to
  #(.renameTo ^File %1 %2))

(def mkdir
  "Create the directory referred to by the given File."
  #(.mkdir ^File %1))

(def pushback-reader
  #(java.io.PushbackReader. ^FileReader %))
