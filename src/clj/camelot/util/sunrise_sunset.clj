(ns camelot.util.sunrise-sunset
  (:require
   [schema.core :as sch]
   [clj-time.coerce :as tc]
   [clj-time.core :as t])
  (:import
   (com.luckycatlabs.sunrisesunset SunriseSunsetCalculator)
   (com.luckycatlabs.sunrisesunset.dto Location)
   (org.joda.time DateTime Seconds)
   (java.util Calendar TimeZone)))

(defn- ->location ^Location
  [^String lat ^String lon]
  (Location. lat lon))

(defn- ->sunrise-sunset-calculator ^SunriseSunsetCalculator
  [^Location l ^TimeZone tz]
  (SunriseSunsetCalculator. l tz))

(defn- ->calendar-for-date ^Calendar
  [^DateTime date]
  (let [cal (java.util.Calendar/getInstance)]
    (.setTime cal (.toDate date))
    cal))

(defn- ->sunrise-calendar ^Calendar
  [^SunriseSunsetCalculator calculator ^Calendar calendar]
  (.getOfficialSunriseCalendarForDate calculator calendar))

(defn- ->sunset-calendar ^Calendar
  [^SunriseSunsetCalculator calculator ^Calendar calendar]
  (.getOfficialSunsetCalendarForDate calculator calendar))

(defn- calendar-time ^DateTime
  [^Calendar cal]
  (.getTime cal))

(defn- tz-offset ^long
  [^TimeZone tz ^Long since-epoch]
  (.getOffset tz since-epoch))

(defn- truncate-tz ^DateTime
  [^Calendar cal ^DateTime date ^TimeZone tz]
  (t/plus (tc/from-date (calendar-time cal))
          (Seconds/seconds (/ (tz-offset tz (tc/to-long date)) 1000))))

(defn- build-sunrise-sunset-time
  [calendar-fn]
  (fn [tz lat lon date]
    (some-> (->location lat lon)
            (->sunrise-sunset-calculator tz)
            (calendar-fn (->calendar-for-date date))
            (truncate-tz date tz))))

(sch/defn sunrise-time :- (sch/maybe DateTime)
  [tz :- TimeZone
   lat :- sch/Str
   lon :- sch/Str
   date :- DateTime]
  (let [f (build-sunrise-sunset-time ->sunrise-calendar)]
    (f tz lat lon date)))

(sch/defn sunset-time :- (sch/maybe DateTime)
  [tz :- TimeZone
   lat :- sch/Str
   lon :- sch/Str
   date :- DateTime]
  (let [f (build-sunrise-sunset-time ->sunset-calendar)]
    (f tz lat lon date)))
