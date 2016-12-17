(ns camelot.bulk-import.validation
  "Validators to be ran before bulk-import."
  (:require
   [clj-time.core :as t]
   [camelot.util.date :as date]
   [camelot.translation.core :as tr]
   [clj-time.format :as tf]
   [clojure.string :as str]))

(defn check-media-within-session-date
  "Return a fail result if the media capture date is outside of the session dates."
  [state record]
  (let [start (:trap-station-session-start-date record)
        end (:trap-station-session-end-date record)
        capture (:media-capture-timestamp record)]
    (cond
      (t/before? capture start)
      {:result :fail}

      (and (t/equal? (date/at-midnight end) end)
           (or (date/at-or-after? capture (t/plus (date/at-midnight end)
                                                  (t/days 1)))))
      {:result :fail}

      (and (not (t/equal? (date/at-midnight end) end))
           (t/after? capture end))
      {:result :fail}

      :else {:result :pass})))

(defn check-session-end-date-not-in-future
  "Return a fail result if the end date is in the future."
  [state record]
  (if (t/after? (:trap-station-session-end-date record) (t/now))
    {:result :fail}
    {:result :pass}))

(defn check-session-start-before-end
  "Return a fail result if the end date occurs before the start date."
  [state record]
  (if (t/after? (:trap-station-session-start-date record)
                (:trap-station-session-end-date record))
    {:result :fail}
    {:result :pass}))

(defn overlap-reducer
  "Collate overlapping dates for a camera."
  [acc n]
  (if (and (:last-end acc)
           (t/before? (:trap-station-session-start-date n) (:last-end acc)))
    (let [extremes (juxt date/earliest date/latest)
          [end-earliest end-latest] (extremes (:last-end acc)
                                              (:trap-station-session-end-date n))]
      (assoc (update acc :overlaps #(conj % {:overlap-start (:trap-station-session-start-date n)
                                             :overlap-end end-earliest}))
             :last-end end-latest))
    (assoc acc :last-end (:trap-station-session-end-date n))))

(defn simplifying-overlap-reducer
  "Collapse overlapping ranges where possible"
  [acc n]
  (if-let [l (:current acc)]
    (if (t/before? (:overlap-start n) (:overlap-end l))
      (update acc :current #(hash-map :overlap-start (date/earliest (:overlap-start n)
                                                                    (:overlap-start %))
                                      :overlap-end (date/latest (:overlap-end n)
                                                                (:overlap-end %))))
      (assoc (update acc :confirmed conj (:current acc))
             :current n))
    (assoc acc :current n)))

(defn simplify-overlap
  "Given a seq of overlaps, return a new seq where overlapping overlaps are collapsed."
  [overlaps]
  (let [finalise #(update % :confirmed conj (:current %))]
    (->> overlaps
         (reduce simplifying-overlap-reducer {})
         finalise
         :confirmed
         (remove nil?))))

(defn check-overlap
  "Return map with camera name and all overlapping dates."
  [[camera entries]]
   (->> entries
        (sort-by :trap-station-session-start-date)
        (reduce overlap-reducer {:overlaps []})
        :overlaps
        simplify-overlap
        (hash-map :camera camera :overlaps)))

(defn overlap-fail-with-reason
  "Return a failure describing the problem overlap."
  [state {:keys [camera overlaps]}]
  {:result :fail
   :reason (tr/translate state ::camera-overlap camera
                         (str/join ", " (map
                                         #(str
                                           (tf/unparse date/day-formatter (:overlap-start %))
                                           " " (tr/translate state :words/and-lc) " "
                                           (tf/unparse date/day-formatter (:overlap-end %)))
                                         overlaps)))})

(defn check-overlapping-camera-usage
  "Map a seq of records to failures, where a camera has usages with overlapping dates."
  [state records]
  (->> records
       (group-by :camera-name)
       (map check-overlap)
       (remove #(empty? (:overlaps %)))
       (map (partial overlap-fail-with-reason state))))

(defn list-record-problems
  "Apply tests to each record, returning all failures."
  ([state records]
   (let [tests {:session-dates check-media-within-session-date
                :future-timestamp check-session-end-date-not-in-future
                :session-start-before-end check-session-start-before-end}]
     (list-record-problems state tests records)))
  ([state tests records]
   (filter #(= (:result %) :fail)
           (apply concat
                  (map-indexed
                   #(map (fn [[t f]] (assoc (f state %2)
                                            :test t
                                            :row (+ %1 2)))
                         tests) records)))))
