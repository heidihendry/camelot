(ns camelot.report.module.presence-util
  "PRESENCE output generation."
  (:require
   [clj-time.core :as t]
   [camelot.report.sighting-independence :as indep]
   [clj-time.format :as tf]
   [camelot.model.survey :as survey]
   [camelot.util.date :as date]
   [clj-time.coerce :as tc]))

(defn list-days
  "Return a list of dates between start-date and end date, exclusive."
  [start-date end-date-excl]
  (loop [{:keys [day acc]} {:day start-date :acc []}]
    (if (t/after? day end-date-excl)
      acc
      (recur {:day (t/plus day (t/days 1)) :acc (conj acc (date/at-midnight day))}))))

(defn data-by-day
  "Transform data in to a map of days and species counts."
  [data]
  (reduce
   (fn [acc d]
     (if (:sighting-quantity d)
       (update acc (date/at-midnight (:media-capture-timestamp d))
               #(+ (or % 0) (:sighting-quantity d)))
       acc))
   {} data))

(defn generate-row
  "Generate a row with one entry for each day."
  [state value-fn taxonomy-id start-date end-excl session-ranges day-list data]
  (let [by-day (->> data
                    (remove #(or (not (:media-capture-timestamp %))
                                 (t/before? (:media-capture-timestamp %) start-date)
                                 (t/after? (:media-capture-timestamp %) end-excl)))
                    (filter #(= (:taxonomy-id %) taxonomy-id))
                    data-by-day)]
    (map (partial value-fn session-ranges by-day) day-list)))

(defn add-date-header
  "Prepend date column headers to the given list."
  [day-list output]
  (cons
   (cons "" (map #(tf/unparse (tf/formatters :year-month-day) %) day-list))
   output))

(defn- record->time-range
  [rec]
  (let [vec-fn (juxt :trap-station-session-start-date :trap-station-session-end-date)]
    (->> (update rec :trap-station-session-end-date #(t/plus % (t/days 1)))
         vec-fn
         (mapv tc/to-long))))

(defn- continuum-reducer
  [acc [start end :as c]]
  (if (empty? acc)
    (conj acc c)
    (let [latest (get-in acc [(dec (count acc)) 1])]
      (if (> start latest)
        (conj acc c)
        (update-in acc [(dec (count acc)) 1] (constantly end))))))

(defn session-date-ranges
  "Return a vector of session date ranges, given a dateset input.
  A date range is the longest continous period of time for which camera trap
  sessions exist. Each date range is a vector. Each date range is guaranteed
  not to overlap in time with any other date range.

  The end-date is, as usual, taken to be 23:59:59 of the date it represents."
  [data]
  (->> data
       (map record->time-range)
       (sort-by (juxt first second))
       (reduce continuum-reducer [])
       (mapv (fn [span] (update (mapv tc/from-long span)
                                1
                                #(t/minus % (t/millis 1)))))))

(defn in-session-range?
  "True if date falls within a session. False otherwise"
  [session-ranges d]
  (some? (some (fn [[s e]] (and (date/at-or-after? d s)
                                (date/at-or-before? d e)))
               session-ranges)))

(defn generate
  [value-fn taxonomy-id start-date end-date state data]
  (let [end-excl (t/plus end-date (t/days 1))
        day-list (list-days start-date end-date)]
    (survey/with-survey-settings [s state]
      (->> data
           (indep/->independent-sightings s)
           (group-by :trap-station-id)
           vals
           (map #(cons (:trap-station-name (first %))
                       (generate-row s value-fn taxonomy-id start-date end-excl
                                     (session-date-ranges %) day-list %)))
           (add-date-header day-list)))))

(defn no-sightings-value
  [session-ranges d]
  (if (in-session-range? session-ranges d)
    0
    "-"))

(def generate-count
  (partial generate (fn [session-ranges by-day x]
                      (or (get by-day x) (no-sightings-value session-ranges x)))))

(def generate-presence
  (partial generate (fn [session-ranges by-day x]
                      (let [v (get by-day x)]
                        (if (and v (pos? v)) 1 (no-sightings-value session-ranges x))))))
