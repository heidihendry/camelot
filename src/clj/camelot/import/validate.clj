(ns camelot.import.validate
  "Validators to be ran before bulk-import."
  (:require
   [clj-time.core :as t]
   [camelot.util.date :as date]
   [camelot.translation.core :as tr]
   [clj-time.format :as tf]
   [clojure.string :as str]
   [camelot.state.datasets :as datasets]
   [camelot.util.file :as file]))

(def record-size-safety-threshold
  "Disk space relative to the raw file size required to upload."
  1.15)

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

(defn scalar-nil-or-empty?
  "Predicate returning true if the value is nil or an empty string. False otherwise."
  [x]
  (if (string? x)
    (empty? x)
    (nil? x)))

(defn check-sighting-assignment
  "Return failure result if any of sighting-quantity or taxonomy species/genus set, but not all."
  [state record]
  (let [s-vals (map #(get record %) [:sighting-quantity
                                     :taxonomy-species
                                     :taxonomy-genus
                                     :taxonomy-common-name])]
    (if (and (some? (some scalar-nil-or-empty? s-vals))
             (not (every? scalar-nil-or-empty? s-vals)))
      {:result :fail}
      {:result :pass})))

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
       (map #(select-keys % [:trap-station-session-start-date
                             :trap-station-session-end-date]))
       distinct
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

(defn check-filesystem-space
  [state records]
  (let [record-size (->> records
                         (map :absolute-path)
                         (map file/length)
                         (reduce + 0))
        avail (->> (datasets/lookup-path (:datasets state) :media)
                   file/->file
                   file/canonical-path
                   file/->file
                   file/fs-usable-space)
        needed (* record-size record-size-safety-threshold)]
    (if (and (pos? avail) (> needed avail))
      [{:result :fail
        :reason (tr/translate state ::filesystem-space
                              (long (/ needed 1024 1024)) "MB"
                              (long (/ avail 1024 1024)) "MB")}]
      [])))

(defn list-record-problems
  "Apply tests to each record, returning all failures."
  ([state records]
   (let [tests {::session-dates check-media-within-session-date
                ::future-timestamp check-session-end-date-not-in-future
                ::session-start-before-end check-session-start-before-end
                ::check-sighting-assignment check-sighting-assignment}]
     (list-record-problems state tests records)))
  ([state tests records]
   (filter #(= (:result %) :fail)
           (apply concat
                  (map-indexed
                   #(map (fn [[t f]] (assoc (f state %2)
                                            :reason (tr/translate state t (+ %1 2))
                                            :test t
                                            :row (+ %1 2)))
                         tests) records)))))

(defn list-dataset-problems
  "Validate records, returning any dataset level problems."
  [state records]
  (let [tests {::camera-overlaps check-overlapping-camera-usage
               ::filesystem-space check-filesystem-space}]
    (mapcat (fn [[t f]] (map #(assoc % :test t) (f state records))) tests)))

(defn validate
  "Validate records."
  [state records]
  (let [vs (juxt list-dataset-problems list-record-problems)]
    (apply concat (vs state records))))
