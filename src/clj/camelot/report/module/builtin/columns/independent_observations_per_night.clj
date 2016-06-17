(ns camelot.report.module.builtin.columns.independent-observations-per-night
  (:require [camelot.report.module.core :as module]))

(defn- get-nights-per-independent-observation
  [record]
  (let [obs (:independent-observations record)
        nights (or (:total-nights record) (:nights-elapsed record))]
    (cond
        (nil? obs) nil
        (or (nil? nights) (zero? nights)) "-"
        :else (format "%.3f" (* 100 (double (/ obs nights)))))))

(defn- calculate-independent-observations-per-night
  [state data]
  (->> data
       (map #(assoc % :independent-observations-per-night
                    (get-nights-per-independent-observation %)))))

(module/register-column
 :independent-observations-per-night
 {:post-aggregate calculate-independent-observations-per-night})
