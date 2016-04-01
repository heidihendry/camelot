(ns camelot.album
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]
            [camelot.model.album :as ma]
            [camelot.photo :as photo]))

(defn check-ir-threshold
  "Check whether the album's photos exceed the user-defined infrared check thresholds."
  [state photos]
  (let [nightfn (partial photo/night? (:night-start-hour (:config state)) (:night-end-hour (:config state)))
        ir-check-fn (partial photo/infrared-sane? nightfn
                             (:infrared-iso-value-threshold (:config state)))
        ir-failed (count (remove identity (map ir-check-fn photos)))
        night-total (count (filter #(nightfn (t/hour (:datetime %))) photos))]
    (if (not (zero? night-total))
      (if (> (/ ir-failed night-total) (:erroneous-infrared-threshold (:config state)))
        :fail
        :pass)
      :pass)))

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
  [state previous-sightings datetime quantity]
  (let [duration (:sighting-independence-minutes-threshold (:config state))]
    (conj previous-sightings {:start datetime
                              :end (t/plus datetime (t/minutes duration))
                              :quantity quantity
                              })))

(defn- update-sighting
  [state previous-sightings sighting quantity]
  (conj previous-sightings
        (assoc sighting :quantity (max (or (get sighting :quantity) 0)
                                       quantity))))

(defn- dependent-sighting
  [sighting datespans]
  (first (filter #(or (= sighting (:start %))
                      (= sighting (:end %))
                      (and (t/after? sighting (:start %))
                           (t/before? sighting (:end %))))
                 datespans)))

(defn- independence-reducer
  [state datetime acc this-sighting]
  (let [species (:species this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))]
    (assoc acc species
           (if previous-sighting
             (update-sighting state (remove #(= previous-sighting %) (get acc species)) previous-sighting (:quantity this-sighting))
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

(s/defn squares
  [avg coll]
  (map #(let [n (-' % avg)] (*' n n)) coll))

(s/defn stddev
  [coll]
  (let [total (count coll)
        mean (/ (reduce +' 0 coll) total)
        squares (squares mean coll)]
    (if (< total 2)
      0
      (Math/sqrt (/ (apply +' squares) (-' total 1))))))

(defn check-photo-stddev
  [state photos]
  (let [photos (sort #(t/before? (:datetime %1) (:datetime %2)) photos)
        gettime #(-> % (:datetime) (tc/to-long))
        ftime (gettime (first photos))
        times (map #(-' (gettime %) ftime) photos)
        sd (stddev times)]
    (if (nil? (reduce #(if (> %2 (+' %1 (*' sd 3)))
                         (reduced nil)
                         %2) 0 (rest times)))
     :fail
     :pass)))

(defn check-project-dates
  [state photos]
  (let [photos (sort #(t/before? (:datetime %1) (:datetime %2)) photos)]
    (if (or (t/before? (:datetime (first photos)) (:project-start (:config state)))
            (t/after? (:datetime (last photos)) (:project-end (:config state))))
      :fail
      :pass)))

(defn check-camera-checks
  [state photos]
  (let [has-check #(some (fn [x] (re-matches #"(?i).*camera.?check" (:species x)))
                         (:sightings %))
        as-day #(let [d (:datetime %)] (t/date-time (t/year d) (t/month d) (t/day d)))
        check-photos (->> photos
                          (filter has-check)
                          (map as-day)
                          (into #{}))]
    (if (> (count check-photos) 1)
      :pass
      :fail)))

(defn check-headline-consistency
  [state photos]
  (or (reduce #(when (not (= (:headline %1) (:headline %2)))
                 (reduced :fail)) (first photos) (rest photos))
      :pass))

(defn check-required-fields
  [state photos]
  (let [fields (:required-fields (:config state))]
    (or (reduce #(when (some nil? (map (partial photo/extract-path-value %2) fields))
                   (reduced :fail)) nil
                   photos)
        :pass)))

(defn check-album-has-data
  [state photos]
  (if (empty? photos)
    :fail
    :pass))

(defn check-sighting-consistency
  [state photos]
  (or (reduce #(if (or (nil? (:quantity %2)) (nil? (:species %2)))
                 (reduced :fail)
                 %1)
              :pass
              (apply concat (map :sightings photos)))
      :pass))

(defn check-species
  [state photos]
  (let [m (->> photos
               (map #(map :species (:sightings %)))
               (flatten)
               (remove nil?)
               (filter #(not (some #{(clojure.string/lower-case %)}
                                   (map clojure.string/lower-case
                                        (:surveyed-species (:config state))))))
               (first))]
    (if m
      :fail
      :pass)))

(defn- problem-descriptions
  [state problems]
  (map #(hash-map :problem % :description ((:translate state) %)) problems))

(s/defn list-problems :- [s/Keyword]
  "Return a list of all problems encountered while processing album data"
  [state album-data]
  (let [tests {:photo-stddev check-photo-stddev
               :project-dates check-project-dates
               :time-light-sanity check-ir-threshold
               :camera-checks check-camera-checks
               :headline-consistency check-headline-consistency
               :required-fields check-required-fields
               :album-has-data check-album-has-data
               :sighting-consistency check-sighting-consistency
               :surveyed-species check-species}]
    (remove nil?
            (map (fn [[t f]]
                   (if (= (f state (vals (:photos album-data))) :fail)
                     t
                     nil))
                 tests))))

(s/defn album :- ma/Album
  "Return the metadata for a single album, given raw tag data"
  [state set-data]
  (let [album-data (into {} (map (fn [[k v]] [k (photo/normalise state v)]) set-data))]
    {:photos album-data
     :metadata (extract-metadata state (vals album-data))
     :problems (problem-descriptions state (list-problems state album-data))}))

(s/defn album-set
  "Return a datastructure representing all albums and their metadata"
  [state tree-data]
  (into {} (map (fn [[k v]] (vector k (album state v))) tree-data)))
