(ns camelot.util.date
  (:require [clj-time.core :as t]))

(defn at-midnight
  "Floor a given date to midnight on that day."
  [date]
  (t/date-time (t/year date) (t/month date) (t/day date)))
