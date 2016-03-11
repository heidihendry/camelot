(ns ctdp.photoset
  (:require [clojure.java.io :as io]
            [clj-time.core :as t]
            [schema.core :as s]
            [ctdp.photo :refer [file-metadata]]
            [cats.monad.either :as either])
  (:import [com.drew.imaging ImageMetadataReader]))

(defn- infrared-sanity
  [sunrise sunset isothresh photo]
  (let [hour (t/hour (:datetime photo))
        iso (:iso (:settings photo))]
    (if (> iso isothresh)
      (or (> hour sunset) (< hour sunrise))
      true)))

(defn- exceed-ir-threshold
  [config photos]
  (let [isothresh (:infrared-iso-value-threshold config)
        ir-fn (partial infrared-sanity (:sunlight-start-hour config)
                       (:sunlight-end-hour config) isothresh)
        ir-check (map ir-fn photos)
        ir-failed (count (remove identity ir-check))
        ir-total (+ (count (filter #(> (:iso (:settings %)) isothresh) photos)) 1)]
    (> (/ ir-failed ir-total) (:erroneous-infrared-threshold config))))

(defn- data-from-photos
  [state filelist]
  (let [photodata (into {} (map #(vector % (file-metadata %)) filelist))
        photos (map (fn [[k v]] v) photodata)]
    (if (exceed-ir-threshold (:config state) photos)
      (either/left ((:translations state) (:language (:config state))
                    :error/infrared-datetime-threshold-exceeded))
      (either/right photodata))))

(defn- exif-data-file
  [file]
  (and (.isFile file) (re-find #"(?i)(.jpe?g|.tiff?)$" (.getName file))))

(defn- exif-data-dir
  [files]
  (and (some exif-data-file files)
       (not (some #(.isDirectory %) files))))

(defn- process-dir
  [state dir]
  (let [files (filter exif-data-file (file-seq (io/file dir)))]
    (vector dir (data-from-photos state files))))

(s/defn data-from-tree :- [s/enum cats.monad.either.Left cats.monad.either.Right]
  "Extract photo metadata from all directories in the root.
Result is grouped by directory.  Only leaves containing (EXIF) photos are
considered valid.  Result is Either an error or a map with the directory name
and the photo metadata."
  [state rootdir]
  {:pre [(instance? java.io.File rootdir)]}
  (let [dirname #(.getParent (io/file %))
        dirs (keys (filter (fn[[k v]] (exif-data-dir v))
                           (group-by dirname (file-seq rootdir))))]
    (into {} (map (partial process-dir state) dirs))))
