(ns camelot.report.module.core
  "Column definitions for the report builder.")

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

(defn get-report
  [report-key]
  (get @known-reports report-key))

(defn all-reports
  []
  (deref known-reports))
