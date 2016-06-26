(ns camelot.report.module.builtin.columns.presence-absence
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(defn- presense-flag
  [v]
  (if (zero? v)
    ""
    "X"))

(defn- aggregate-presense-absence
  [col data]
  (->> data
       (col-util/aggregate-by-trap-station-session :independent-observations)
       (presense-flag)))

(defn- calculate-presence-absence
  [state data]
  (->> data
       (col-util/calculate-independent-observations state)
       (map #(assoc % :presence-absence
                    (presense-flag (:independent-observations %))))))

(module/register-column
 :presence-absence
 {:calculate calculate-presence-absence
  :aggregate aggregate-presense-absence})
