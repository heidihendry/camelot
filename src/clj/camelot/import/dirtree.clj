(ns camelot.import.dirtree
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [camelot.model.state :refer [State]]
            [schema.core :as s]
            [camelot.model.import :as mi]
            [camelot.util.java-file :as f]
            [camelot.util.file :as file-util]
            [camelot.util.java-file :as jf])
  (:import [com.drew.imaging ImageMetadataReader]
           [org.apache.commons.lang3 SystemUtils]
           [com.drew.metadata Metadata Directory Tag]
           [java.io File]))

(def RawAlbum {java.io.File mi/ImportRawMetadata})

(def RawAlbumSet {java.io.File RawAlbum})

(def path-component-prefix "Path Component ")
(def absolute-path-key "Absolute Path")

(def file-inclusion-regexp
    "Regexp for filenames to include.
  `file-exclusion-regexp' takes precedence."
    #"(?i)(.jpe?g|.tiff?)$")

(def file-exclusion-regexp
  "Regexp file filenames to exclude.
  Takes precedence over `file-inclusion-regexp'."
   #"(?i)_original(.jpe?g|.tiff?)$")

(defn get-description
  [tag]
  (.getDescription ^Tag tag))

(defn get-tag-name
  [tag]
  (.getTagName ^Tag tag))

(defn get-tags
  [directory]
  (.getTags ^Directory directory))

(defn get-directories
  [metadata]
  (.getDirectories ^Metadata metadata))

(s/defn exif-file? :- s/Bool
  "Predicate for whether a given file is a usable, exif-containing file."
  [file]
  (or (and (f/file? file)
           (f/readable? file)
           (re-find file-inclusion-regexp (f/get-name file))
           (not (re-find file-exclusion-regexp (f/get-name file)))
           true)
      false))

(defn- album-dir?
  "Return true if there are exif-containing files and the directory hasn't any subdirectories. False otherwise."
  [files]
  (and (some exif-file? files)
       (not-any? f/directory? files)))

(defn- describe-raw-tag
  "Return a description for the given tag."
  [tag]
  (or (some-> tag
              get-description
              str/trim)
      ""))

(defn- tag-key-value-pair
  "Return the key-value pair for the raw metadata tag given."
  [tag]
  (hash-map (get-tag-name tag) (describe-raw-tag tag)))

(defn- parse-tag
  "Map tag names to their descriptions, returning the result as a hash"
  [tag]
  (into {} (map tag-key-value-pair tag)))

(defn- extract-tags
  "Extract a list of raw tags from the directories of metadata."
  [metadata]
  (map get-tags (get-directories metadata)))

(s/defn file-metadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [reader file]
  (->> file
       (reader)
       (extract-tags)
       (map parse-tag)
       (into {})))

(s/defn exif-files-in-dir :- [File]
  "Return a list of the exif files in dir."
  [dir :- s/Str]
  (->> dir
       (io/file)
       (file-seq)
       (filter exif-file?)))

(s/defn file-raw-metadata :- mi/ImportRawMetadata
  [state file]
  (let [reader #(ImageMetadataReader/readMetadata ^File %)]
    (try
      (file-metadata reader file)
      (catch java.lang.Exception e {}))))

(s/defn path-components :- mi/ImportRawMetadata
  "Extract a map of components of the path, relative to the root directory."
  [state :- State
   file :- File]
  (let [segfn (fn [i v] (hash-map (str path-component-prefix (inc i)) v))]
    (->> file
         (file-util/rel-path-components state)
         (map-indexed segfn)
         (apply merge))))

(s/defn file-metadata :- mi/ImportRawMetadata
  "Return a pair of the file and its raw metadata."
  [state :- State
   file :- File]
  (merge (file-raw-metadata state file)
         (path-components state file)
         {absolute-path-key (jf/canonical-path file)}))

(s/defn file-metadata-pair
  [state :- State
   file :- File]
  (vector file (file-metadata state file)))

(s/defn directory-metadata-collection :- [mi/ImportRawMetadata]
  [state :- State
   dir :- s/Str]
  (->> dir
       exif-files-in-dir
       (map (partial file-metadata state))))

(s/defn album-dir-raw-metadata :- RawAlbum
  "Return the raw exif data for files in `dir'."
  [state :- State
   dir :- s/Str]
  (->> dir
       (exif-files-in-dir)
       (map (partial file-metadata-pair state))
       (remove nil?)
       (into {})))

(defn- album-dir-list
  "Return a list of valid album directories under `root'."
  [root]
  (let [dirname #(f/get-parent-file (io/file %))]
    (->> root
         (io/file)
         (file-seq)
         (group-by dirname)
         (filter (fn [[k v]] (album-dir? v)))
         (keys))))

(defn- dir-raw-album-pair
  "Return a pair consisting of the directory and the raw album data."
  [state dir]
  (vector dir (album-dir-raw-metadata state dir)))

(s/defn read-tree :- RawAlbumSet
  "Extract photo metadata from all directories in the root.
Result is grouped by directory.  Only leaves containing (EXIF) photos are
considered valid.  Result is Either an error or a map with the directory name
and the photo metadata."
  [state root]
  {:pre [(string? root)]}
  (->> root
       (album-dir-list)
       (map (partial dir-raw-album-pair state))
       (into {})))
