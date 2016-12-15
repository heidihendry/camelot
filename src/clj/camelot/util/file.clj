(ns camelot.util.file
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)
   (org.apache.commons.lang3 SystemUtils)))

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

(defn delete
  [file]
  (when (exists? file)
    (.delete ^File file)))

(defn list-files
  [dir]
  (.listFiles ^File dir))

(defn basename
  "Return the of a file basename as a string.  If given a pattern as the second argument, will remove it."
  ([file] (get-name file))
  ([file pattern]
   (str/replace (get-name file) pattern "")))

(defn delete-recursive
  "Remove a directory and its content recursively."
  [file]
  (if (directory? file)
    (when (reduce #(and %1 (delete-recursive %2)) true (list-files file))
      (delete file))
    (delete file)))

(defn length
  [file]
  "Returns the number of bytes in the file"
  (.length ^File file))

(defn canonical-path
  [file]
  "Return the absolute, unique path to the file as a String."
  (.getCanonicalPath ^File file))

(defn pushback-reader
  [file-reader]
  "Create a pushback reader given a file reader"
  (java.io.PushbackReader. ^FileReader file-reader))

(defn rename
  "Rename the file at source to dest."
  [source dest]
  (.renameTo ^File source ^File dest))

(defn path-separator
  "Return the path separator for the OS Camelot is running upon."
  []
  (if SystemUtils/IS_OS_WINDOWS
    "\\"
    "/"))

(defn- path-separator-re
  []
  (if SystemUtils/IS_OS_WINDOWS
    #"\\"
    #"/"))

(defn ^File ->file
  "Return a File object from `path`."
  [path]
  (io/file path))

(defn rel-path-components
  "Return the relative path to `file' as a list of strings, each string representing a component of the path."
  [state file]
  (let [store (get-in state [:config :store])
        rp (canonical-path (io/file (:root-path @store)))]
    (str/split (subs (canonical-path file) (inc (count rp)))
               (path-separator-re))))