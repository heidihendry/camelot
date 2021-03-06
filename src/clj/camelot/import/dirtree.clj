(ns camelot.import.dirtree
  (:require
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.file :as file]
   [clojure.string :as str]
   [schema.core :as sch])
  (:import
   (com.drew.imaging ImageMetadataReader)
   (com.drew.metadata Metadata Directory Tag)
   (java.io File)))

(def ImportRawMetadata {sch/Str sch/Str})
(def RawAlbum {java.io.File ImportRawMetadata})

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

(sch/defn exif-file? :- sch/Bool
  "Predicate for whether a given file is a usable, exif-containing file."
  [file]
  (or (and (file/file? file)
           (file/readable? file)
           (re-find file-inclusion-regexp (file/get-name file))
           (not (re-find file-exclusion-regexp (file/get-name file)))
           true)
      false))

(defn- album-dir?
  "Return true if there are exif-containing files and the directory hasn't any subdirectories. False otherwise."
  [files]
  (and (some exif-file? files)
       (not-any? file/directory? files)))

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

(sch/defn extract-file-metadata :- ImportRawMetadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [reader file]
  (or (some->> file
               (reader)
               (extract-tags)
               (map parse-tag)
               (into {}))
      ""))

(sch/defn exif-files-in-dir :- [File]
  "Return a list of the exif files in dir."
  [dir :- sch/Str]
  (->> dir
       (file/->file)
       (file-seq)
       (filter exif-file?)))

(sch/defn file-raw-metadata :- ImportRawMetadata
  [state file]
  (let [reader #(ImageMetadataReader/readMetadata ^File %)]
    (try
      (extract-file-metadata reader file)
      (catch java.lang.Exception _ {}))))

(sch/defn path-components :- ImportRawMetadata
  "Extract a map of components of the path, relative to the root directory."
  [state :- State
   file :- File]
  (let [segfn (fn [i v] (hash-map (str path-component-prefix (inc i)) v))]
    (->> file
         (file/rel-path-components state)
         (map-indexed segfn)
         (apply merge))))

(sch/defn file-metadata :- ImportRawMetadata
  "Return a pair of the file and its raw metadata."
  [state :- State
   file :- File]
  (merge (file-raw-metadata state file)
         (path-components state file)
         {absolute-path-key (file/canonical-path file)}))

(sch/defn file-metadata-pair
  [state :- State
   file :- File]
  (vector file (file-metadata state file)))

(sch/defn directory-metadata-collection :- [ImportRawMetadata]
  [state :- State
   dir :- sch/Str]
  (->> dir
       exif-files-in-dir
       (map (partial file-metadata state))))

(sch/defn album-dir-raw-metadata :- RawAlbum
  "Return the raw exif data for files in `dir'."
  [state :- State
   dir :- sch/Str]
  (->> dir
       (exif-files-in-dir)
       (map (partial file-metadata-pair state))
       (remove nil?)
       (into {})))

(defn- album-dir-list
  "Return a list of valid album directories under `root'."
  [root]
  (let [dirname #(file/get-parent-file (file/->file %))]
    (->> root
         (file/->file)
         (file-seq)
         (group-by dirname)
         (filter (fn [[_ v]] (album-dir? v)))
         (keys))))

(defn- dir-raw-album-pair
  "Return a pair consisting of the directory and the raw album data."
  [state dir]
  (vector dir (album-dir-raw-metadata state dir)))

(sch/defn read-tree :- RawAlbumSet
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
