(ns camelot.processing.album
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.util.java-file :as jf]
            [camelot.translation.core :as tr]
            [camelot.processing.dirtree :as dt]
            [camelot.model.photo :as mp]
            [camelot.model.album :as ma]
            [camelot.processing.photo :as photo]
            [camelot.processing.validation :refer [list-problems check-invalid-photos]]
            [clojure.java.io :as io]
            [camelot.handler.trap-station-session-cameras :as trap-station-session-cameras]))

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

(defn- add-sighting
  "Add a new (i.e., independent) sighting."
  [state previous-sightings datetime quantity]
  (let [duration (:sighting-independence-minutes-threshold (:config state))]
    (conj previous-sightings {:start datetime
                              :end (t/plus datetime (t/minutes duration))
                              :quantity quantity})))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [previous-sightings sighting quantity]
  (let [new-qty (max (or (get sighting :quantity) 0) quantity)]
    (conj previous-sightings
          (assoc sighting :quantity new-qty))))

(defn- dependent-sighting?
  "Predicate for whether the sighting would be dependent for a timespan."
  [sighting timespan]
  (or (= sighting (:start timespan))
      (and (t/after? sighting (:start timespan))
           (t/before? sighting (:end timespan)))))

(defn- dependent-sighting
  "Return the first dependent sighting, if any."
  [sighting datespans]
  (first (filter (partial dependent-sighting? sighting) datespans)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state acc this-sighting]
  (let [datetime (:media-capture-timestamp this-sighting)
        species (:species-scientific-name this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))
        qty (:sighting-quantity this-sighting)
        known-sightings (get acc species)]
    (assoc acc species
           (if previous-sighting
             (update-sighting (remove #(= previous-sighting %) known-sightings)
                              previous-sighting qty)
             (add-sighting state known-sightings datetime qty)))))

(defn- add-times-to-sightings
  "Assoc date/time information into the sighting."
  [p]
  (map #(assoc % :datetime (:datetime p)) (:sightings p)))

(s/defn datetime-comparison :- s/Bool
  "Predicate for whether photo-a is prior to photo-b.
`f' is a function applied to both prior to the comparison."
  [f ta tb]
  (t/after? (get tb f) (get ta f)))

(defn album-photos
  "Return a list of photos in album."
  [album]
  (vals (:photos (second album))))

(s/defn extract-independent-sightings :- [ma/Sightings]
  "Extract the sightings, accounting for the independence threshold, for an album."
  [state sightings]
  (let [indep-reducer (partial independence-reducer state)
        total-spp (fn [[spp data]] {:species spp
                                    :count (reduce + (map :quantity data))})]
    (->> sightings
         (sort (partial datetime-comparison :media-capture-timestamp))
         (reduce indep-reducer {})
         (map total-spp))))

(s/defn extract-metadata :- ma/ExtractedMetadata
  "Return aggregated metadata for a given album"
  [state album]
  {:datetime-start ((extract-start-date) album)
   :datetime-end ((extract-end-date) album)
   :make (extract-make album)
   :model (extract-model album)})

(s/defn album :- ma/Album
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

(s/defn album-set :- {java.io.File ma/Album}
  "Return a datastructure representing all albums and their metadata"
  [state tree-data]
  (let [to-album (fn [[k v]] (hash-map k (album state v)))]
    (into {} (mapv to-album tree-data))))

(defn imported-album?
  [state [file _]]
  (nil? (trap-station-session-cameras/get-specific-by-import-path
         state (.toString file))))

(defn read-albums
  "Read photo directories and return metadata structured as albums."
  [state dir]
  (let [fdir (io/file dir)]
    (cond
      (nil? dir) (tr/translate (:config state) :problems/root-path-missing)
      (not (jf/readable? fdir)) (tr/translate (:config state) :problems/root-path-not-found)
      :else
      (->> dir
           (dt/read-tree state)
           (album-set state)
           (filter (partial imported-album? state))
           (into {})))))
