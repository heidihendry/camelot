(ns om-datepicker.dates)

(defn- truncate-tz
  ([]
   (truncate-tz (js/Date.)))
  ([date]
   (let [year (.getFullYear date)
         month (.getMonth date)
         day (.getDate date)
         hour (.getHours date)
         minute (.getMinutes date)
         second (.getSeconds date)]
     (js/Date. (.UTC js/Date year month day hour minute second)))))

(defn truncate-time
  [date]
  (let [year (.getFullYear date)
         month (.getMonth date)
         day (.getDate date)]
    (js/Date. (.UTC js/Date year month day))))

(defn leap-year?
  [year]
  (or (and (= 0 (mod year 4))
           (not (= 0 (mod year 100))))
      (= 0 (mod year 400))))

(defn days-in-month
  [year month]
  (get [31 (if (leap-year? year) 29 28) 31 30 31 30 31 31 30 31 30 31] month))

(defn date-instance
  [date]
  (truncate-tz (js/Date. (.getUTCFullYear date) (.getUTCMonth date) (.getUTCDate date))))

(defn today
  []
  (date-instance (truncate-tz)))

(defn first-of-month
  [date]
  (truncate-tz (js/Date. (.getUTCFullYear date) (.getUTCMonth date) 1)))

(defn last-of-month
  [date]
  (let [year (.getUTCFullYear date)
        month (.getUTCMonth date)]
    (truncate-tz (js/Date. year month (days-in-month year month)))))

(defn switch-date!
  [date offset]
  (.setUTCDate date (+ (.getUTCDate date) offset))
  date)

(defn current-month
  []
  (first-of-month (truncate-tz)))

(defn add-months
  [date months]
  (let [day  (.getUTCDate date)
        date (truncate-tz (js/Date. (.getUTCFullYear date)
                                    (+ (.getUTCMonth date) months) 1))]
    (.setUTCDate date (min day (days-in-month (.getUTCFullYear date) (.getUTCMonth date))))
    date))

(defn next-month
  [date]
  (add-months date 1))

(defn next-year
  [date]
  (add-months date 12))

(defn previous-month
  [date]
  (add-months date -1))

(defn previous-year
  [date]
  (add-months date -12))

(defn coerse-date
  [date]
  (if (number? date)
    (switch-date! (today) date)
    date))

(defprotocol DateTimeProtocol
  (after? [this that])
  (before? [this that])
  (between? [this start end])
  (is-future? [this])
  (same-month? [this that]))

(extend-protocol DateTimeProtocol
  js/Date
  (after? [this that]
    (> (.getTime this) (.getTime that)))

  (before? [this that]
    (<= (.getTime this) (.getTime that)))

  (between? [this start end]
    (and (>= (.getTime this)
             (.getTime start))
         (<= (.getTime this)
             (.getTime end))))

  (is-future? [this]
    (> (.getTime this) (.getTime (truncate-tz))))

  (same-month? [this that]
    (and (= (.getUTCFullYear this) (.getUTCFullYear that))
         (= (.getUTCMonth this) (.getUTCMonth that)))))
