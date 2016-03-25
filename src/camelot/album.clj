(ns camelot.album
  (:require [clj-time.core :as t]
            [schema.core :as s]
            [camelot.model.album :as ma]
            [camelot.photo :as photo]))

(s/defn exceed-ir-threshold :- s/Bool
  "Check whether the album's photos exceed the user-defined infrared check thresholds."
  [config photos]
  (let [nightfn (partial photo/night? (:night-start-hour config) (:night-end-hour config))
        ir-check-fn (partial photo/infrared-sane? nightfn
                             (:infrared-iso-value-threshold config))
        ir-failed (count (remove identity (map ir-check-fn photos)))
        night-total (count (filter #(nightfn (t/hour (:datetime %))) photos))]
    (if (not (zero? night-total))
      (> (/ ir-failed night-total) (:erroneous-infrared-threshold config))
      false)))

(s/defn list-problems :- [s/Keyword]
  "Return a list of all problems encountered while processing album data"
  [config album-data]
  (if (exceed-ir-threshold config (map (fn [[k v]] v) album-data))
    [:datetime]
    []))

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

(s/defn extract-metadata :- ma/ExtractedMetadata
  "Return aggregated metadata for a given album"
  [album]
  {:datetime-start ((extract-start-date) album)
   :datetime-end ((extract-end-date) album)
   :make (extract-make album)
   :model (extract-model album)})

(s/defn album :- ma/Album
  "Return the metadata for a single album, given raw tag data"
  [state set-data]
  (let [album-data (into {} (map (fn [[k v]] [k (photo/normalise v)]) set-data))]
    {:photos album-data
     :metadata (extract-metadata (vals album-data))
     :problems (list-problems (:config state) album-data)}))

(s/defn album-set
  "Return a datastructure representing all albums and their metadata"
  [state tree-data]
  (into {} (map (fn [[k v]] (vector k (album state v))) tree-data)))
