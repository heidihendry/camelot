(ns camelot.util.date
  (:require [clj-time.core :as t]))

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
