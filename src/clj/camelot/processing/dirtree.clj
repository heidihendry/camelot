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

(defn- exif-file?
  "Predicate for whether a given file is a usable, exif-containing file."
  [file]
  (and (f/file? file) (f/readable? file) (re-find #"(?i)(.jpe?g|.tiff?)$" (f/get-name file))
       (not (re-find #"(?i)_original(.jpe?g|.tiff?)$" (f/get-name file)))))

(defn- album-dir?
  "Return true if there are exif-containing files and the directory hasn't any subdirectories. False otherwise."
  [files]
  (and (some exif-file? files)
       (not (some f/directory? files))))

(defn- parse-tag
  "Map tag names to their descriptions, returning the result as a hash"
  [tag]
  (into {} (map #(hash-map (im/getTagName %) (->> % (im/getDescription) (str/trim))) tag)))

(s/defn file-metadata :- ma/RawMetadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [reader file]
  (let [metadata (reader file)
        tags (map im/getTags (im/getDirectories metadata))]
    (into {} (map parse-tag tags))))

(s/defn exif-files :- RawAlbum
  [state dir]
  (let [files (filter exif-file? (file-seq (io/file dir)))
        reader #(ImageMetadataReader/readMetadata ^File %)]
    (into {} (map #(vector % (file-metadata reader %)) files))))

(s/defn read-tree :- RawAlbumSet
  "Extract photo metadata from all directories in the root.
Result is grouped by directory.  Only leaves containing (EXIF) photos are
considered valid.  Result is Either an error or a map with the directory name
and the photo metadata."
  [state root]
  {:pre [(string? root)]}
  (let [dirname #(f/get-parent-file (io/file %))
        dirs (keys (filter (fn [[k v]] (album-dir? v))
                           (group-by dirname (file-seq (clojure.java.io/file root)))))]
    (into {} (map #(vector % (exif-files state %)) dirs))))
