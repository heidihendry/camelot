(ns ctdp.album-reader
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [schema.core :as s]
            [ctdp.album :refer [album]]
            [cats.monad.either :as either])
  (:import [com.drew.imaging ImageMetadataReader]))

(defn- exif-file?
  [file]
  (and (.isFile file) (re-find #"(?i)(.jpe?g|.tiff?)$" (.getName file))))

(defn- album-dir?
  [files]
  (and (some exif-file? files)
       (not (some #(.isDirectory %) files))))

(defn- parse-tag
  [tag]
  (into {} (map #(hash-map (.getTagName %) (-> % (.getDescription) (str/trim))) tag)))

(defn metadata
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [file]
  (let [metadata (ImageMetadataReader/readMetadata file)
        tags (map #(.getTags %) (.getDirectories metadata))]
    (into {} (map parse-tag tags))))

(defn directory-to-album
  [state dir]
  (let [files (filter exif-file? (file-seq (io/file dir)))
        dataset (into {} (map #(vector % (metadata %)) files))]
    [dir (album state dataset)]))

(s/defn data-from-tree :- [s/enum cats.monad.either.Left cats.monad.either.Right]
  "Extract photo metadata from all directories in the root.
Result is grouped by directory.  Only leaves containing (EXIF) photos are
considered valid.  Result is Either an error or a map with the directory name
and the photo metadata."
  [state rootdir]
  {:pre [(instance? java.io.File rootdir)]}
  (let [dirname #(.getParent (io/file %))
        dirs (keys (filter (fn[[k v]] (album-dir? v))
                           (group-by dirname (file-seq rootdir))))]
    (into {} (map (partial directory-to-album state) dirs))))
