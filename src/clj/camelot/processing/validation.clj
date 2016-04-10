(ns camelot.processing.validation
  (:require [camelot.processing.photo :as photo]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
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
  [state photos]
  (if (empty? photos)
    {:result :pass}
    (let [photos (sort #(t/before? (:datetime %1) (:datetime %2)) photos)]
      (cond (t/before? (:datetime (first photos)) (:project-start (:config state)))
            {:result :fail :reason ((:translate state) :checks/project-date-before (:filename (first photos)))}
            (t/after? (:datetime (last photos)) (:project-end (:config state)))
            {:result :fail :reason ((:translate state) :checks/project-date-after (:filename (first photos)))}
            :else {:result :pass}))))

(defn check-camera-checks
  "Ensure there are at least two camera-checks in the given set of photos with unique dates."
  [state photos]
  (let [has-check #(some (fn [x] (re-matches #"(?i).*camera.?check" (:species x)))
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
    (reduced {:result :fail :reason ((:translate state) :checks/headline-consistency (:filename h1) (:filename h2))})
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
  [state photos]
  (let [fields (:required-fields (:config state))]
    (or (reduce #(when (some nil? (map (partial photo/extract-path-value %2) fields))
                   (reduced {:result :fail
                             :reason ((:translate state)
                                      :checks/required-fields (:filename %2))}))
                nil photos)
        {:result :pass})))

(defn check-album-has-data
  "Ensure the album has data."
  [state photos]
  (if (empty? photos)
    {:result :fail
     :reason ((:translate state) :checks/album-has-data)}
    {:result :pass}))

(defn check-sighting-consistency
  "Ensure the sighting data is fully completed."
  [state photos]
  (or (reduce (fn [acc p] (if (or (some #(nil? (:quantity %)) (:sightings p))
                                  (some #(nil? (:species %)) (:sightings p)))
                        (reduced {:result :fail :reason ((:translate state)
                                                         :checks/sighting-consistency
                                                         (:filename p))})
                        acc))
              {:result :pass}
              photos)
      {:result :pass}))

(defn check-species
  "Ensure the species of the photos are known to the survey"
  [state photos]
  (let [m (->> photos
               (map #(hash-map :file (:filename %)
                               :species (map :species (:sightings %))))
               (flatten)
               (remove #(empty? (:species %)))
               (filter #(not (every? (into #{} (map clojure.string/lower-case
                                                    (:surveyed-species (:config state))))
                                     (map clojure.string/lower-case (:species %)))))
               (first))]
    (if m
      {:result :fail
       :reason ((:translate state) :checks/surveyed-species (:file m))}
      {:result :pass})))

(defn check-future
  "Ensure the timestamp on the photos is not in the future."
  [state photos]
  (let [res (filter #(t/after? (:datetime %) (t/now)) photos)]
    (if (empty? res)
      {:result :pass}
      {:result :fail
       :reason ((:translate state) :checks/future-timestamp
                (:filename (first res)))})))

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
               :surveyed-species check-species
               :future-timestamp check-future}]
    (remove nil?
            (map (fn [[t f]]
                   (let [res (f state (vals album-data))]
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
