(ns camelot.util.date
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]))

(def day-formatter (tf/formatter "yyyy-MM-dd"))

(defn at-midnight
  "Floor a given date to midnight on that day."
  [date]
  (t/date-time (t/year date) (t/month date) (t/day date)))

(defn at-or-before?
  "Return true if date-a is at the same time as or before date-b."
  [date-a date-b]
  (if (or (t/equal? date-a date-b)
          (t/before? date-a date-b))
    true
    false))

(defn at-or-after?
  "Return true if date-a is at the same time as or after date-b."
  [date-a date-b]
  (if (or (t/equal? date-a date-b)
          (t/after? date-a date-b))
    true
    false))

(defn latest
  "Return the latest of the given dates."
  ([date-a date-b]
   (if (t/after? date-a date-b)
     date-a
     date-b))
  ([date-a date-b & dates]
   (reduce latest date-a (conj (into '() dates) date-b))))

(defn earliest
  "Return the earliest of the given dates."
  ([date-a date-b]
   (if (t/before? date-a date-b)
     date-a
     date-b))
  ([date-a date-b & dates]
   (reduce earliest date-a (conj (into '() dates) date-b))))
