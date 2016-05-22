(ns camelot.processing.album
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.model.album :as ma]
            [camelot.processing.photo :as photo]
            [camelot.processing.validation :refer [list-problems check-invalid-photos]]))

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
  (let [datetime (:datetime this-sighting)
        species (:species this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))
        qty (:quantity this-sighting)
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
  "Predicate for whether photo-a is prior to photo-b."
  [ta tb]
  (t/after? (:datetime tb) (:datetime ta)))

(s/defn extract-independent-sightings :- [ma/Sightings]
  "Extract the sightings, accounting for the independence threshold, for an album."
  [state album]
  (let [indep-reducer (partial independence-reducer state)
        total-spp (fn [[spp data]] {:species spp
                                    :count (reduce + (map :quantity data))})]
    (->> album
         (map add-times-to-sightings)
         (flatten)
         (sort datetime-comparison)
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
  (let [to-album (fn [[k v]] (vector k (album state v)))]
    (into {} (map to-album tree-data))))
