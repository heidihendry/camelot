(ns camelot.processing.album
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.model.album :as ma]
            [camelot.processing.photo :as photo]
            [camelot.processing.validation :refer [list-problems]]))

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
                              :quantity quantity
                              })))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [previous-sightings sighting quantity]
  (conj previous-sightings
        (assoc sighting :quantity (max (or (get sighting :quantity) 0)
                                       quantity))))

(defn- dependent-sighting
  "Return the first dependent sighting, if any."
  [sighting datespans]
  (first (filter #(or (= sighting (:start %))
                      (= sighting (:end %))
                      (and (t/after? sighting (:start %))
                           (t/before? sighting (:end %))))
                 datespans)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state datetime acc this-sighting]
  (let [species (:species this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))]
    (assoc acc species
           (if previous-sighting
             (update-sighting (remove #(= previous-sighting %) (get acc species)) previous-sighting (:quantity this-sighting))
             (add-sighting state (get acc species) datetime (:quantity this-sighting))))))

(defn extract-independent-sightings
  "Extract the camera model from an album"
  [state album]
  (into {} (map (fn [[k v]] {k (reduce + (map :quantity v))})
                (reduce #(reduce (partial independence-reducer state (:datetime %2)) %1
                                 (:sightings %2)) {} (sort #(t/before? (:datetime %1)
                                                                       (:datetime %2))
                                                           album)))))

(s/defn extract-metadata :- ma/ExtractedMetadata
  "Return aggregated metadata for a given album"
  [state album]
  {:datetime-start ((extract-start-date) album)
   :datetime-end ((extract-end-date) album)
   :make (extract-make album)
   :model (extract-model album)
   :sightings (extract-independent-sightings state album)})

(s/defn album :- ma/Album
  "Return the metadata for a single album, given raw tag data"
  [state set-data]
  (let [album-data (into {} (map (fn [[k v]] [k (photo/parse state v)]) set-data))]
    {:photos album-data
     :metadata (extract-metadata state (vals album-data))
     :problems (list-problems state album-data)}))

(s/defn album-set
  "Return a datastructure representing all albums and their metadata"
  [state tree-data]
  (into {} (map (fn [[k v]] (vector k (album state v))) tree-data)))
