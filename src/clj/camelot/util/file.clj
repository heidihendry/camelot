(ns camelot.util.file
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)
   (org.apache.commons.io FilenameUtils)
   (org.apache.commons.lang3 SystemUtils)))

(defn get-parent
  "Return the parent as a path."
  [file]
  (.getParent ^File file))

(defn get-parent-file
  "Return the parent as a file."
  [file]
  (.getParentFile ^File file))

(defn get-name
  "Return the name of the given file."
  [file]
  (.getName ^File file))

(defn directory?
  "Predicate for whether the file is a directory."
  [file]
  (.isDirectory ^File file))

(defn to-path
  [file]
  (.toPath ^File file))

(defn file?
  "Predicate for whether the File object is a file."
  [file]
  (.isFile ^File file))

(defn get-path
  "Return the path to the given file as a String."
  [file]
  (.getPath ^File file))

(defn exists?
  "Predicate for whether the file exists"
  [file]
  (.exists ^File file))

(defn readable?
  "Predicate for whether the given file is readable."
  [file]
  (.canRead ^File file))

(defn writable?
  "Predicate for whether the given file is readable."
  [file]
  (.canWrite ^File file))

(defn mkdir
  "Create the directory referred to by the given File."
  [file]
  (.mkdir ^File file))

(defn mkdirs
  "Create the directory, and all parent directories, referred to by the given File."
  [file]
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

(defn extension
  "Return the extension of the file as a string, sans the period."
  [^String file]
  (FilenameUtils/getExtension file))

(defn delete-recursive
  "Remove a directory and its content recursively."
  [file]
  (if (directory? file)
    (when (reduce #(and %1 (delete-recursive %2)) true (list-files file))
      (delete file))
    (delete file)))

(defn length
  "Returns the number of bytes in the file"
  [file]
  (.length ^File file))

(defn fs-usable-space
  "Return the number of available bytes on the volume where `file' lives."
  [^File file]
  (.getUsableSpace file))

(defn canonical-path
  "Return the absolute, unique path to the file as a String."
  [file]
  (.getCanonicalPath ^File file))

(defn pushback-reader
  "Create a pushback reader given a file reader"
  [file-reader]
  (java.io.PushbackReader. ^java.io.Reader file-reader))

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
  "Return a File object from `paths`."
  ([& paths]
   (apply io/file paths)))

(defn rel-path-components
  "Return the relative path to `file' as a list of strings, each string representing a component of the path."
  [state file]
  (let [store (get-in state [:config :store])
        crp (if-let [rp (:root-path @store)]
              (canonical-path (io/file rp))
              "")]
    (str/split (subs (canonical-path file) (inc (count crp)))
               (path-separator-re))))
