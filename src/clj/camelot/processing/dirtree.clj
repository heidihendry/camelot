(ns camelot.processing.dirtree
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s]
            [camelot.model.album :as ma]
            [camelot.util.java-file :as f]
            [camelot.util.image-metadata :as im])
  (:import [com.drew.imaging ImageMetadataReader]
           [camelot.model.photo PhotoMetadata]
           [java.io File]))

(def RawAlbum {java.io.File ma/RawMetadata})

(def RawAlbumSet {java.io.File RawAlbum})

(def file-inclusion-regexp
    "Regexp for filenames to include.
  `file-exclusion-regexp' takes precedence."
    #"(?i)(.jpe?g|.tiff?)$")

(def file-exclusion-regexp
  "Regexp file filenames to exclude.
  Takes precedence over `file-inclusion-regexp'."
   #"(?i)_original(.jpe?g|.tiff?)$")

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
  (->> tag
       (im/getDescription)
       (str/trim)))

(defn- tag-key-value-pair
  "Return the key-value pair for the raw metadata tag given."
  [tag]
  (hash-map (im/getTagName tag) (describe-raw-tag tag)))

(defn- parse-tag
  "Map tag names to their descriptions, returning the result as a hash"
  [tag]
  (into {} (map tag-key-value-pair tag)))

(defn- extract-tags
  "Extract a list of raw tags from the directories of metadata."
  [metadata]
  (map im/getTags (im/getDirectories metadata)))

(s/defn file-metadata :- ma/RawMetadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [reader file]
  (->> file
       (reader)
       (extract-tags)
       (map parse-tag)
       (into {})))

(defn- exif-files-in-dir
  "Return a list of the exif files in dir."
  [dir]
  (->> dir
       (io/file)
       (file-seq)
       (filter exif-file?)))

(s/defn file-raw-metadata-pair
  "Return a pair of the file and its raw metadata."
  [reader file]
  (vector file (file-metadata reader file)))

(s/defn album-dir-raw-metadata :- RawAlbum
  "Return the raw exif data for files in `dir'."
  [state dir]
  (let [reader #(ImageMetadataReader/readMetadata ^File %)]
    (->> dir
         (exif-files-in-dir)
         (map (partial file-raw-metadata-pair reader))
         (into {}))))

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
