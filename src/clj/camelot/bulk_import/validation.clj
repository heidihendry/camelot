(ns camelot.bulk-import.validation
  "Validators to be ran before bulk-import."
  (:require
   [clj-time.core :as t]
   [camelot.util.date :as date]))

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
