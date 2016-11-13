(ns camelot.report.module.builtin.columns.media-capture-time-breakdown
  (:require
   [camelot.report.module.core :as module]
   [clj-time.core :as t]
   [clj-time.format :as tf]))

(def date-formatter (tf/formatter "YYYY-MM-dd"))
(def time-formatter (tf/formatter "HH:mm:ss"))

(defn calculate-media-capture-date
  [state data]
  (map
   #(if-let [ts (:media-capture-timestamp %)]
      (assoc % :media-capture-date
             (tf/unparse date-formatter ts))
      %) data))

(defn calculate-media-capture-time
  [state data]
  (map
   #(if-let [ts (:media-capture-timestamp %)]
      (assoc % :media-capture-time
             (tf/unparse time-formatter ts))
      %) data))

(module/register-column
 :media-capture-date
 {:calculate calculate-media-capture-date})

(module/register-column
 :media-capture-time
 {:calculate calculate-media-capture-time})
