(ns camelot.report.module.builtin.columns.time-period
  (:require
   [camelot.report.module.column-util :as col-util]
   [camelot.report.module.core :as module]
   [clj-time.core :as t]
   [clj-time.format :as tf]))

(def date-formatter (tf/formatter "YYYY-MM-dd"))

(defn before-reducer
  "Reducer returning the earliest date-time."
  ([] nil)
  ([a b]
   (cond (nil? a) b
         (nil? b) a
         (pos? (compare a b)) b
         :else a)))

(defn after-reducer
  "Reducer returning the latest date-time."
  ([] nil)
  ([a b]
   (cond (nil? a) b
         (nil? b) a
         (neg? (compare a b)) b
         :else a)))

(defn date->period
  [from-col to-col state data]
  (map #(assoc % to-col
               (some->> (get % from-col)
                        (tf/unparse date-formatter)))
       data))

(module/register-column
 :time-period-start
 {:calculate (partial date->period :trap-station-session-start-date :time-period-start)
  :aggregate (partial col-util/aggregate-with-reducer before-reducer :trap-station-session-id)})

(module/register-column
 :time-period-end
 {:calculate (partial date->period :trap-station-session-end-date :time-period-end)
  :aggregate (partial col-util/aggregate-with-reducer after-reducer :trap-station-session-id)})
