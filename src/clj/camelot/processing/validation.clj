(ns camelot.processing.validation
  (:require [camelot.processing.photo :as photo]
            [camelot.processing.util :as putil]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.string :as str]
            [camelot.util.java-file :as jf]
            [incanter.core :as incanter]
            [incanter.stats :as istats]
            [schema.core :as s]))

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
        {:result :fail :reason ((:translate state) :checks/time-light-sanity)}
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
        (first below) {:result :fail :reason
                       ((:translate state) :checks/stddev-before (:filename (first below)))}
        (first above) {:result :fail :reason
                       ((:translate state) :checks/stddev-after (:filename (first above)))}
        :else {:result :pass}))))

(defn check-project-dates
  "Check that photos fall within the defined project dates."
  [state photo]
  (let [datetime (:datetime photo)
        file (:filename photo)]
    (cond (t/before? datetime (:project-start (:config state)))
          {:result :fail
           :reason ((:translate state) :checks/project-date-before file)}
          (t/after? datetime (:project-end (:config state)))
          {:result :fail
           :reason ((:translate state) :checks/project-date-after file)}
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
      {:result :fail :reason ((:translate state) :checks/camera-checks)})))

(defn compare-headlines
  "Compare two handlines, failing if they're not consistent."
  [state h1 h2]
  (if (not (= (:headline h1) (:headline h2)))
    (reduced {:result :fail :reason ((:translate state) :checks/headline-consistency
                                     (:filename h1) (:filename h2))})
    h1))

(defn check-headline-consistency
  "Check the headline of all photos is consistent."
  [state photos]
  (let [r (reduce (partial compare-headlines state) (first photos) (rest photos))]
    (if (= (:result r) :fail)
      r
      {:result :pass})))

(defn check-required-fields
  "Ensure all Require Fields contain data."
  [state photo]
  (let [fields (:required-fields (:config state))
        missing (filter #(nil? (photo/extract-path-value photo %)) fields)]
    (if (empty? missing)
      {:result :pass}
      {:result :fail
       :reason ((:translate state)
                :checks/required-fields (:filename photo) (str/join ", " (map #(putil/path-description state %) missing)))})))

(defn check-album-has-data
  "Ensure the album has data."
  [state photos]
  (if (empty? photos)
    {:result :fail
     :reason ((:translate state) :checks/album-has-data)}
    {:result :pass}))

(defn sightings-reducer
  [state photo]
  (fn [acc sighting]
    (let [special #"(?i)\bcamera-?check\b|\bunknown\b"]
      (cond
        (and (nil? (:quantity sighting))
             (nil? (re-find special (:species sighting))))
        (reduced {:result :fail
                  :reason ((:translate state)
                           :checks/sighting-consistency-quantity-needed
                           (:filename photo))})
        (nil? (:species sighting))
        (reduced {:result :fail
                  :reason ((:translate state)
                           :checks/sighting-consistency-species-needed
                           (:filename photo))})))))

(defn check-sighting-consistency
  "Ensure the sighting data is fully completed."
  [state photo]
  (or (reduce (sightings-reducer state photo) nil (:sightings photo))
      {:result :pass}))

(defn check-species
  "Ensure the species of the photos are known to the survey"
  [state photo]
  (let [m (->> (:sightings photo)
               (map :species)
               (remove #(re-find #"(?i)\bcamera-?check\b|\bunknown\b" %))
               (map str/lower-case)
               (remove (into #{} (map str/lower-case (:surveyed-species (:config state))))))]
    (if (empty? m)
      {:result :pass}
      {:result :fail
       :reason ((:translate state) :checks/surveyed-species (:filename photo)
                (str/join ", " m))})))

(defn check-future
  "Ensure the timestamp on the photos is not in the future."
  [state photo]
  (if (t/after? (:datetime photo) (t/now))
    {:result :fail
     :reason ((:translate state) :checks/future-timestamp
              (:filename photo))}
    {:result :pass}))

(defn check-invalid-photos
  "Check the album for invalid photos"
  [state photos]
  (let [res (first (into [] (filter #(contains? % :invalid) photos)))]
    (if (nil? res)
      {:result :pass}
      {:result :fail
       :reason ((:translate state) :checks/invalid-photos (:invalid res))})))

(defn check-timeshift-consistency
  "Check that any timeshift applied to files is consistent across the album."
  [state photos]
  (if (empty? photos)
    {:result :pass}
    (let [shifts (->> photos
                      (map #(vector (:filename %)
                                    (photo/get-timeshift (:datetime-original %)
                                                         (:datetime %))))
                      (group-by second)
                      (into []))]
      (if (= (count shifts) 1)
        {:result :pass}
        {:result :fail
         :reason ((:translate state) :checks/inconsistent-timeshift
                  (ffirst (second (first shifts)))
                  (ffirst (second (second shifts))))}))))

(s/defn list-photo-problems
  [state photos]
  (let [tests {:project-dates check-project-dates
               :required-fields check-required-fields
               :sighting-consistency check-sighting-consistency
               :surveyed-species check-species
               :future-timestamp check-future}]
    (filter #(= (:result %) :fail)
            (flatten (map #(map (fn [[t f]] (f state %)) tests) photos)))))

(s/defn list-album-problems
  "Return a list of all problems encountered at an album level"
  [state photos]
  (let [tests {:photo-stddev check-photo-stddev
               :time-light-sanity check-ir-threshold
               :camera-checks check-camera-checks
               :headline-consistency check-headline-consistency
               :album-has-data check-album-has-data
               :timeshift-consistency check-timeshift-consistency}]
    (remove nil?
            (map (fn [[t f]]
                   (let [res (f state photos)]
                     (if (not= (:result res) :pass)
                       {:problem t
                        :reason (if (:reason res)
                                  (:reason res)
                                  (->> t
                                       (name)
                                       (str "checks/")
                                       (keyword)
                                       ((:translate state))
                                       ((:translate state) :checks/problem-without-reason)))}
                       nil)))
                 tests))))

(s/defn list-problems
  "Return a list of all problems encountered while processing `album-data'"
  [state album-data]
  (let [photos (vals album-data)
        baseline-check (check-invalid-photos state photos)]
    (if (= (:result baseline-check) :fail)
      [baseline-check]
      (concat (list-album-problems state photos)
              (list-photo-problems state photos)))))
