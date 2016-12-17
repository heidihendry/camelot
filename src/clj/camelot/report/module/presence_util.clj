(ns camelot.report.module.presence-util
  "PRESENCE output generation."
  (:require
   [clj-time.core :as t]
   [camelot.report.sighting-independence :as indep]
   [clj-time.format :as tf]
   [camelot.util.date :as date]))

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
       (update acc (to-day (:media-capture-timestamp d))
               #(+ (or % 0) (:sighting-quantity d)))
       acc))
   {} data))

(defn generate-row
  "Generate a row with one entry for each day."
  [state value-fn taxonomy-id start-date end-excl day-list data]
  (let [by-day (->> data
                    (remove #(or (not (:media-capture-timestamp %))
                                 (t/before? (:media-capture-timestamp %) start-date)
                                 (t/after? (:media-capture-timestamp %) end-excl)))
                    (filter #(= (:taxonomy-id %) taxonomy-id))
                    data-by-day)]
    (map (partial value-fn by-day) day-list)))

(defn add-date-header
  "Prepend date column headers to the given list."
  [day-list output]
  (cons
   (cons "" (map #(tf/unparse (tf/formatters :year-month-day) %) day-list))
   output))

(defn generate
  [value-fn taxonomy-id start-date end-date state data]
  (let [end-excl (t/plus end-date (t/days 1))
        day-list (list-days start-date end-date)]
    (->> data
         (indep/->independent-sightings state)
         (group-by :trap-station-id)
         vals
         (map #(cons (:trap-station-name (first %))
                     (generate-row state value-fn taxonomy-id start-date end-excl day-list %)))
         (add-date-header day-list))))

(def generate-count
  (partial generate (fn [by-day x] (or (get by-day x) 0))))

(def generate-presence
  (partial generate (fn [by-day x]
                      (let [v (get by-day x)]
                        (if (and v (pos? v)) 1 0)))))
