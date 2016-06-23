(ns camelot.util.java-file
  (:import [java.io File]))

(defn get-parent
  [file]
  "Return the parent as a path."
  (.getParent ^File file))

(defn get-parent-file
  [file]
  "Return the parent as a file."
  (.getParentFile ^File file))

(defn get-name
  [file]
  "Return the name of the given file."
  (.getName ^File file))

(defn directory?
  [file]
  "Predicate for whether the file is a directory."
  (.isDirectory ^File file))

(defn to-path
  [file]
  (.toPath ^File file))

(defn file?
  [file]
  "Predicate for whether the File object is a file."
  (.isFile ^File file))

(defn get-path
  [file]
  "Return the path to the given file as a String."
  (.getPath ^File file))

(defn exists?
  [file]
  "Predicate for whether the file exists"
  (.exists ^File file))

(defn readable?
  [file]
  "Predicate for whether the given file is readable."
  (.canRead ^File file))

(defn writable?
  [file]
  "Predicate for whether the given file is readable."
  (.canWrite ^File file))

(defn mkdir
  [file]
  "Create the directory referred to by the given File."
  (.mkdir ^File file))

(defn mkdirs
  [file]
  "Create the directory, and all parent directories, referred to by the given File."
  (.mkdirs ^File file))

(defn pushback-reader
  [file-reader]
  "Create a pushback reader given a file reader"
  (java.io.PushbackReader. ^FileReader file-reader))
