(ns camelot.report.module.core
  "Column definitions for the report builder."
  (:require
   [camelot.translation.core :as tr]))

(defonce known-columns
  (atom {}))

(defonce known-reports
  (atom {}))

(defn- calculate-column
  [state t acc c]
  (let [f (get-in @known-columns [c t])]
    (if f
      (f state acc)
      acc)))

(defn register-column
  [k conf]
  (swap! known-columns assoc k conf))

(defn build-calculated-columns
  [t]
  (fn [state columns data]
    (let [cols (filter (set (keys @known-columns)) columns)]
      (reduce (partial calculate-column state t) data cols))))

(defn register-report
  [k conf]
  (swap! known-reports assoc k conf))

(defn report-for-state
  [state report]
  (-> report
      (update :title #(if (keyword? %)
                        (tr/translate (:config state) %)
                        %))
      (update :description #(if (keyword? %)
                              (tr/translate (:config state) %)
                              %))
      (update :form #(if (fn? %)
                       (% state)
                       %))))

(defn get-report
  [state report-key]
  (report-for-state state (get @known-reports report-key)))

(defn all-reports
  [state]
  (reduce-kv (fn [acc k v]
               (assoc acc k (report-for-state state v))) {} @known-reports))
