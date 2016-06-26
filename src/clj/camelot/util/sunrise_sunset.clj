(ns camelot.util.sunrise-sunset
  (:require [schema.core :as s]
            [clj-time.coerce :as tc]
            [clj-time.local :as tl]
            [clj-time.core :as t])
  (:import [com.luckycatlabs.sunrisesunset SunriseSunsetCalculator]
           [com.luckycatlabs.sunrisesunset.dto Location]
           [org.joda.time DateTime Seconds]
           [java.util TimeZone]))

(s/defn calendar-for-date
  [date :- DateTime]
  (let [cal (java.util.Calendar/getInstance)]
    (.setTime cal (.toDate date))
    cal))

(s/defn get-sunrise-time
  [tz :- TimeZone
   lat :- s/Str
   lon :- s/Str
   date :- DateTime]
  (let [l (Location. lat lon)]
    (t/plus
     (tc/from-date
      (.getTime (.getOfficialSunriseCalendarForDate (SunriseSunsetCalculator. l tz)
                                                    (calendar-for-date date))))
     (Seconds/seconds (/ (.getOffset tz (tc/to-long date)) 1000)))))

(s/defn get-sunset-time
  [tz :- TimeZone
   lat :- s/Str
   lon :- s/Str
   date :- DateTime]
  (let [l (Location. lat lon)]
    (t/plus (tc/from-date
             (.getTime (.getOfficialSunsetCalendarForDate (SunriseSunsetCalculator. l tz)
                                                          (calendar-for-date date))))
            (Seconds/seconds (/ (.getOffset tz (tc/to-long date)) 1000)))))
