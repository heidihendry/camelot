(ns camelot.report.core
  "Generate a report from a DSL."
  (:require [camelot
             [application :as app]
             [db :as db]]
            [camelot.report.module.core :as module]
            [camelot.translation.core :as tr]
            [camelot.util.config :as config]
            [clj-time
             [local :as tl]
             [format :as tf]]
            [clojure.data.csv :as csv]
            [clojure.set :as set]
            [ring.util.response :as r]
            [schema.core :as s]
            [yesql.core :as sql]))

(sql/defqueries "sql/reports.sql" {:connection db/spec})

(defn- get-all-by-survey
  []
  (db/clj-keys (-get-all-by-survey)))

(defn- get-all-by-species
  []
  (db/clj-keys (-get-all-by-species)))

(defn- get-all-by-site
  []
  (db/clj-keys (-get-all-by-site)))

(defn- get-all-by-camera
  []
  (db/clj-keys (-get-all-by-camera)))

(def ^:private query-fn-map
  {:survey get-all-by-survey
   :species get-all-by-species
   :site get-all-by-site
   :camera get-all-by-camera})

(defn get-by
  [by]
  ((get query-fn-map by)))

(defn- fill-keys
  [columns data]
  (map
   #(reduce (fn [acc c]
              (if (contains? acc c)
                acc
                (assoc acc c nil)))
            %
            columns)
   data))

(defn- distinct-for-known-keys
  [test-rec comp-rec]
  (cond
    (= test-rec comp-rec) true
    (every? #(= (test-rec %) (comp-rec %)) (keys test-rec)) false
    :else true))

(defn- distinct-in-results
  [result-set testrec]
  (every? #(distinct-for-known-keys testrec %) result-set))

(defn- project
  [columns data]
  (let [results (set/project data columns)]
    (->> results
         (filter #(distinct-in-results results %))
         (fill-keys columns)
         (into #{}))))

(defn- apply-filters
  [filters record]
  (if (seq filters)
    (every? #(% record) filters)
    true))

(defn- filter-records
  [state filters data]
  (filter (partial apply-filters filters) data))

(defn- aggregate-reducer
  [group acc c]
  (let [f (get-in @module/known-columns [c :aggregate])]
    (if f
      (assoc acc c (f c group))
      acc)))

(defn- aggregate-groups
  [aggregated-columns group]
  (let [col-vals (reduce (partial aggregate-reducer group) {} aggregated-columns)
        update-vals #(reduce-kv (fn [acc col v]
                                  (if (nil? (acc col))
                                    acc
                                    (assoc acc col v)))
                                % col-vals)]
    (map update-vals group)))

(defn- aggregate-data
  [columns aggregated-columns data]
  (let [anchors (remove (set aggregated-columns) columns)
        groups (group-by #(select-keys % anchors) data)]
    (->> groups
         (reduce-kv (fn [acc k v]
                      (assoc acc k (aggregate-groups aggregated-columns v)))
                    {})
         (vals)
         (flatten))))

(defn- compare-records
  [order-by a b]
  (->> order-by
       (map #(fn [x y] (compare (get x %) (get y %))))
       (map #(% a b))
       (cons (compare (into [] a) (into [] b)))
       (reduce #(if (zero? %2)
                  %1
                  (reduced %2)) 0)))

(defn- sort-result
  [order-by data]
  (if (seq order-by)
    (apply sorted-set-by (partial compare-records order-by) data)
    data))

(defn- transform-records
  [state transforms data]
  (map
   (fn [r] (reduce #(%2 %1) r transforms))
   data))

(def add-calculated-columns (module/build-calculated-columns :calculate))
(def add-post-aggregate-columns (module/build-calculated-columns :post-aggregate))

(defn generate-report
  "Generate a report given a configuration and data."
  [state {:keys [columns pre-transforms pre-filters transforms filters aggregate-on order-by]} data]
  (->> data
       (add-calculated-columns state columns)
       (transform-records state pre-transforms)
       (filter-records state pre-filters)
       (aggregate-data columns aggregate-on)
       (add-post-aggregate-columns state columns)
       (transform-records state transforms)
       (filter-records state filters)
       (project columns)
       (sort-result order-by)))

(defn- cons-headings
  [state columns data]
  (cons (map #(or (get-in @module/known-columns [% :heading])
                  (tr/translate (:config state)
                                (keyword (str "report/" (name %))))) columns)
        data))

(defn- as-row
  [state cols row]
  (map #(get row %) cols))

(defn as-rows
  [state params data]
  (let [cols (:columns params)]
    (map (partial as-row state cols) data)))

(s/defn to-csv-string :- s/Str
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (.toString io-str)))

(defn exportable-report
  "Generate a report as a CSV."
  [state params data]
  (->> data
       (generate-report state params)
       (as-rows state params)
       (cons-headings state (:columns params))
       (to-csv-string)))

(defn report
  [report-key state id data]
  (let [report (module/get-report report-key)
        conf ((:configuration report) state id)]
    (->> data
         (generate-report state conf)
         (as-rows state conf))))

(defn csv-report
  [report-key state id data]
  (let [report (module/get-report report-key)]
    (exportable-report
     state
     ((:configuration report) state id)
     data)))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(defn content-disposition
  [report survey-id]
  (format "attachment; filename=\"%s_ID_%d_%s.csv\""
          (get report :file-prefix)
          survey-id
          (tf/unparse time-formatter (tl/local-now))))

(defn export
  "Handler for an export request."
  [report-key survey-id]
  (let [report (module/get-report report-key)
        state (app/gen-state (config/config))
        sightings (get-by (:by report))
        data (csv-report report-key state survey-id sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition"
                  (content-disposition report survey-id)))))