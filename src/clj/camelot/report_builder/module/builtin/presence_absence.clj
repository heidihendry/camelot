(ns camelot.report-builder.module.builtin.presence-absence
  (:require [camelot.report-builder.module.core :as module]
            [camelot.report-builder.module.column-util :as col-util]))

(defn- presense-flag
  [v]
  (if (zero? v)
    ""
    "X"))

(defn- aggregate-presense-absence
  [col data]
  (->> data
       (col-util/aggregate-by-trap-station :independent-observations)
       (presense-flag)))

(defn- calculate-presence-absence
  [state data]
  (->> data
       (col-util/calculate-independent-observations state)
       (map #(assoc % :presence-absence
                    (presense-flag (:independent-observations %))))))

(module/add-column
 :presence-absence
 {:calculate calculate-presence-absence
  :aggregate aggregate-presense-absence})
