(ns camelot.report-builder.module.core
  "Column definitions for the report builder."
  (:require [camelot.import.album :as album]
            [clj-time.core :as t]
            [schema.core :as s]))

(defonce known-columns
  (atom {}))

(defn- calculate-column
  [state t acc c]
  (let [f (get-in @known-columns [c t])]
    (if f
      (f state acc)
      acc)))

(defn add-column
  [k conf]
  (swap! known-columns assoc k conf))

(defn build-calculated-columns
  [t]
  (fn [state columns data]
    (let [cols (filter (set (keys @known-columns)) columns)]
      (reduce (partial calculate-column state t) data cols))))
