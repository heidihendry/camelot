(ns camelot.import.album
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [schema.core :as s]
   [camelot.util.file :as file]
   [camelot.translation.core :as tr]
   [camelot.import.dirtree :as dt]
   [camelot.import.model :as mi]
   [camelot.import.photo :as photo]
   [camelot.util.config :as config]
   [camelot.import.validation :refer [list-problems check-invalid-photos]]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]))

(defn- extract-date
  "Extract the first date from an album, given a custom comparison function `cmp'."
  [cmp album]
  (:datetime (first (sort #(cmp (:datetime %1) (:datetime %2)) album))))

(defn- extract-start-date
  "Extract the earliest photo date from the contents of an album"
  []
  (partial extract-date t/before?))

(defn- extract-end-date
  "Extract the most recent photo date from the contents of an album"
  []
  (partial extract-date t/after?))

(defn- extract-make
  "Extract the camera make from an album"
  [album]
  (:make (:camera (first album))))

(defn- extract-model
  "Extract the camera model from an album"
  [album]
  (:model (:camera (first album))))

(defn album-photos
  "Return a list of photos in album."
  [album]
  (vals (:photos (second album))))

(s/defn extract-metadata :- mi/ImportExtractedMetadata
  "Return aggregated metadata for a given album"
  [state album]
  {:datetime-start ((extract-start-date) album)
   :datetime-end ((extract-end-date) album)
   :make (extract-make album)
   :model (extract-model album)})

(s/defn album :- mi/ImportAlbum
  "Return the metadata for a single album, given raw tag data"
  [state set-data]
  (let [parse-photo (fn [[k v]] [k (photo/parse state v)])
        album-data (into {} (map parse-photo set-data))]
    (if (= (:result (check-invalid-photos state (vals album-data))) :fail)
      {:photos album-data
       :invalid true
       :problems (list-problems state album-data)}
      {:photos album-data
       :metadata (extract-metadata state (vals album-data))
       :problems (list-problems state album-data)})))

(defn imported-album?
  [state file]
  (nil? (trap-station-session-camera/get-specific-by-import-path
         state (subs (.toString file)
                     (count (config/lookup state :root-path))))))

(s/defn album-set :- {java.io.File mi/ImportAlbum}
  "Return a datastructure representing all albums and their metadata"
  [state tree-data]
  (let [to-album (fn [[k v]] (hash-map k (album state v)))
        is-imported-fn (fn [[k v]] (imported-album? state k))]
    (into {} (mapv to-album (filter is-imported-fn tree-data)))))

(defn read-albums
  "Read photo directories and return metadata structured as albums."
  [state dir]
  (let [fdir (file/->file dir)]
    (cond
      (nil? dir) (tr/translate state :problems/root-path-missing)
      (not (file/readable? fdir)) (tr/translate state :problems/root-path-not-found)
      :else
      (->> dir
           (dt/read-tree state)
           (album-set state)
           (into {})))))
