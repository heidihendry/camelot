(ns camelot.report.module.builtin.columns.presence-absence
  (:require
   [camelot.report.module
    [column-util :as col-util]
    [core :as module]]))

(defn- presence-flag
  [v]
  (if (zero? v)
    ""
    "X"))

(defn aggregate-presence-absence
  [state col data]
  (->> data
       (col-util/aggregate-by-trap-station-session state :independent-observations)
       (presence-flag)))

(defn calculate-presence-absence
  [state data]
  (->> data
       (col-util/calculate-independent-observations state)
       (map #(assoc % :presence-absence
                    (presence-flag (:independent-observations %))))))

(module/register-column
 :presence-absence
 {:calculate calculate-presence-absence
  :aggregate aggregate-presence-absence})
