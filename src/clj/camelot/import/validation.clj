(ns camelot.import.validation
  (:require
   [camelot.import.photo :as photo]
   [camelot.import.util :as putil]
   [camelot.translation.core :as tr]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [clojure.string :as str]
   [incanter.stats :as istats]
   [schema.core :as s]
   [camelot.util.config :as config]))

(def sighting-quantity-exclusions-re
  #"(?i)\bcamera-?check\b|\bunknown\b|\bunidentified\b")

(defn check-ir-threshold
  "Check whether the album's photos exceed the user-defined infrared check thresholds."
  [state photos]
  (let [nightfn (partial photo/night? (config/lookup state :night-start-hour)
                         (config/lookup state :night-end-hour))
        ir-check-fn (partial photo/infrared-sane? nightfn
                             (config/lookup state :infrared-iso-value-threshold))
        ir-failed (count (remove identity (map ir-check-fn photos)))]
    (if-let [night-violations (seq (filter #(nightfn (t/hour (:datetime %))) photos))]
      (if (> (/ ir-failed (count night-violations))
             (config/lookup state :erroneous-infrared-threshold))
        {:result :warn :reason (tr/translate state :checks/time-light-sanity)}
        {:result :pass})
      {:result :pass})))

(defn check-photo-stddev
  "Check the given photos have no outliers by date/time"
  [state photos]
  (if (< (count photos) 2)
    {:result :pass}
    (let [ms #(-> % :datetime (tc/to-long))
          ptimes (map ms photos)
          [_ p25 _ p75 _] (istats/quantile ptimes)
          stddev (istats/sd ptimes)
          belowt (- p25 (* 3 stddev))
          below (filter #(< (ms %) belowt) photos)
          abovet (+ p75 (* 3 stddev))
          above (filter #(> (ms %) abovet) photos)]
      (cond
        (first below) {:result :warn :reason
                       (tr/translate state :checks/stddev-before (:filename (first below)))}
        (first above) {:result :warn :reason
                       (tr/translate state :checks/stddev-after (:filename (first above)))}
        :else {:result :pass}))))

(defn check-project-dates
  "Check that photos fall within the defined project dates."
  [state photo]
  (let [datetime (:datetime photo)
        file (:filename photo)]
    (cond (t/before? datetime (config/lookup state :project-start))
          {:result :fail
           :reason (tr/translate state :checks/project-date-before file)}
          (t/after? datetime (config/lookup state :project-end))
          {:result :fail
           :reason (tr/translate state :checks/project-date-after file)}
          :else {:result :pass})))

(defn check-camera-checks
  "Ensure there are at least two camera-checks in the given set of photos with unique dates."
  [state photos]
  (let [has-check #(some (fn [x] (if (nil? (:species x))
                                   false
                                   (re-matches #"(?i).*camera.?check" (:species x))))
                         (:sightings %))
        as-day #(let [d (:datetime %)] (t/date-time (t/year d) (t/month d) (t/day d)))
        check-photos (->> photos
                          (filter has-check)
                          (map as-day)
                          (into #{}))]
    (if (> (count check-photos) 1)
      {:result :pass}
      {:result :warn :reason (tr/translate state :checks/camera-checks)})))

(defn compare-headlines
  "Compare two handlines, failing if they're not consistent."
  [state h1 h2]
  (if (not= (:headline h1) (:headline h2))
    (reduced {:result :fail :reason (tr/translate state :checks/headline-consistency
                                     (:filename h1) (:filename h2))})
    h1))

(defn check-headline-consistency
  "Check the headline of all photos is consistent."
  [state photos]
  (let [r (reduce (partial compare-headlines state) (first photos) (rest photos))]
    (if (= (:result r) :fail)
      r
      {:result :pass})))

(defn compare-sources
  "Compare two source fields, failing if they're not consistent."
  [state h1 h2]
  (if (not= (:source h1) (:source h2))
    (reduced {:result :fail :reason (tr/translate state :checks/source-consistency
                                     (:filename h1) (:filename h2))})
    h1))

(defn check-source-consistency
  "Check the source of all photos is consistent."
  [state photos]
  (let [r (reduce (partial compare-sources state) (first photos) (rest photos))]
    (if (= (:result r) :fail)
      r
      {:result :pass})))

(defn compare-cameras
  "Compare two cameras, failing if they're not consistent."
  [state h1 h2]
  (if (or (not= (:make (:camera h1)) (:make (:camera h2)))
          (not= (:model (:camera h1)) (:model (:camera h2)))
          (not= (:software (:camera h1)) (:software (:camera h2))))
    (reduced {:result :warn :reason (tr/translate state :checks/camera-consistency
                                                  (:filename h1) (:filename h2))})
    h1))

(defn check-camera-consistency
  "Check the camera used for all photos is consistent."
  [state photos]
  (let [r (reduce (partial compare-cameras state) (first photos) (rest photos))]
    (if (= (:result r) :warn)
      r
      {:result :pass})))

(defn check-required-fields
  "Ensure all Require Fields contain data."
  [state photo]
  (let [fields (config/lookup state :required-fields)]
    (if-let [missing (seq (filter #(nil? (photo/extract-path-value photo %)) fields))]
      {:result :fail
       :reason (tr/translate state
                             :checks/required-fields (:filename photo) (str/join ", " (map #(putil/path-description state %) missing)))}
      {:result :pass})))

(defn check-album-has-data
  "Ensure the album has data."
  [state photos]
  (if (empty? photos)
    {:result :fail
     :reason (tr/translate state :checks/album-has-data)}
    {:result :pass}))

(defn sightings-reducer
  "Reduce sighting validity.
  A sighting is considered valid if it has both a species and quantity.
  'Special' species (those matching `sighting-quantity-exclusions-re') are
  exempt from this condition."
  [state photo]
  (fn [acc sighting]
    (cond
      (and (nil? (:quantity sighting))
           (nil? (re-find sighting-quantity-exclusions-re (:species sighting))))
      (reduced {:result :fail
                :reason (tr/translate state
                         :checks/sighting-consistency-quantity-needed
                         (:filename photo))})
      (nil? (:species sighting))
      (reduced {:result :fail
                :reason (tr/translate state
                         :checks/sighting-consistency-species-needed
                         (:filename photo))}))))

(defn check-sighting-consistency
  "Ensure the sighting data is fully completed."
  [state photo]
  (or (reduce (sightings-reducer state photo) nil (:sightings photo))
      {:result :pass}))

(defn check-species
  "Ensure the species of the photos are known to the survey"
  [state photo]
  (if-let [m (seq (->> (:sightings photo)
                    (map :species)
                    (remove #(or (nil? %) (re-find sighting-quantity-exclusions-re %)))
                    (map str/lower-case)
                    (remove (set (map str/lower-case
                                      (config/lookup state :surveyed-species))))))]
    {:result :fail
     :reason (tr/translate state :checks/surveyed-species (:filename photo)
                           (str/join ", " m))}
    {:result :pass}))

(defn check-future
  "Ensure the timestamp on the photos is not in the future."
  [state photo]
  (if (t/after? (:datetime photo) (t/now))
    {:result :fail
     :reason (tr/translate state :checks/future-timestamp
              (:filename photo))}
    {:result :pass}))

(defn check-invalid-photos
  "Check the album for invalid photos."
  [state photos]
  (if-let [res (first (filter #(contains? % :invalid) photos))]
    {:result :fail
     :reason (tr/translate state :checks/invalid-photos (:invalid res))}
    {:result :pass}))

(defn check-location-gps-set
  [state photo]
  (let [loc (:location photo)]
    (if (or (nil? (:gps-longitude loc))
            (nil? (:gps-latitude loc)))
      {:result :fail :reason (tr/translate state :checks/gps-data-missing
                                           (:filename photo))})))

(s/defn list-photo-problems
  [state photos]
  (let [tests {:project-dates check-project-dates
               :required-fields check-required-fields
               :sighting-consistency check-sighting-consistency
               :surveyed-species check-species
               :future-timestamp check-future
               :location-gps-set check-location-gps-set}]
    (filter #(= (:result %) :fail)
            (flatten (map #(map (fn [[t f]] (f state %)) tests) photos)))))

(s/defn list-album-problems
  "Return a list of all problems encountered at an album level"
  [state photos]
  (let [tests {:photo-stddev check-photo-stddev
               :time-light-sanity check-ir-threshold
               :camera-checks check-camera-checks
               :headline-consistency check-headline-consistency
               :source-consistency check-source-consistency
               :camera-consistency check-camera-consistency
               :album-has-data check-album-has-data}]
    (remove nil?
            (map (fn [[t f]]
                   (let [res (f state photos)]
                     (when (not= (:result res) :pass)
                       (assoc res
                              :reason (if (:reason res)
                                        (:reason res)
                                        (->> t
                                             (name)
                                             (str "checks/")
                                             (keyword)
                                             (tr/translate state)
                                             (tr/translate state :checks/problem-without-reason)))))))
                 tests))))

(s/defn list-problems
  "Return a list of all problems encountered while processing `album-data'"
  [state album-data]
  (let [photos (vals album-data)
        baseline-check (check-invalid-photos state photos)]
    (if (= (:result baseline-check) :fail)
      [baseline-check]
      (into (list-album-problems state photos)
            (list-photo-problems state photos)))))
