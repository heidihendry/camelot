(ns camelot.report-builder.module.builtin.independent-observations-per-night
  (:require [camelot.report-builder.module.core :as module]))

(defn- get-nights-per-independent-observation
  [record]
  (let [obs (:independent-observations record)
        nights (:nights-elapsed record)]
    (cond
        (nil? obs) nil
        (or (nil? nights) (zero? nights)) "-"
        :else (format "%.3f" (* 100 (double (/ obs nights)))))))

(defn- calculate-independent-observations-per-night
  [state data]
  (->> data
       (map #(assoc % :independent-observations-per-night
                    (get-nights-per-independent-observation %)))))

(module/add-column
 :independent-observations-per-night
 {:post-aggregate calculate-independent-observations-per-night})
