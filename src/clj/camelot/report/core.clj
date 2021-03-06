(ns camelot.report.core
  "Generate a report from a DSL."
  (:require
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.sighting-fields :as util.sf]
   [camelot.report.query :as query]
   [camelot.report.module.loader :as loader]
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]
   [clj-time.local :as tl]
   [clj-time.format :as tf]
   [clojure.data.csv :as csv]
   [clojure.set :as set]
   [ring.util.response :as r]
   [schema.core :as sch]
   [camelot.util.model :as model])
  (:import
   (clojure.lang IFn)))

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

(defn- distinct-in-results
  [results]
  (loop [{:keys [acc check against]} {:acc [] :against {} :check results}]
    (if (empty? check)
      acc
      (if-let [cur (first check)]
        (if (some empty? (map (fn [[k v]] (get-in against [k v])) cur))
          (recur {:check (rest check)
                  :against (reduce-kv (fn [acc k v]
                                        (update-in acc [k v] #(conj % cur)))
                                      against cur)
                  :acc (conj acc cur)})
          (recur {:check (rest check) :against against :acc acc}))))))

(defn- project
  [columns data]
  (let [results (if (= (first columns) :all)
                  (set data)
                  (set/project data columns))]
    (->> results
         (sort-by count >)
         distinct-in-results
         (fill-keys columns)
         (into #{}))))

(defn- apply-filters
  [filters record]
  (every? #(% record) filters))

(defn- filter-records
  [state filters data]
  (if (seq filters)
    (filter (partial apply-filters filters) data)
    data))

(defn aggregate-reducer
  [state group acc c]
  (let [f (get-in @module/known-columns [c :aggregate])]
    (if f
      (assoc acc c (f state c group))
      acc)))

(defn- aggregate-groups
  [state aggregated-columns group]
  (let [col-vals (reduce (partial aggregate-reducer state group) {} aggregated-columns)
        update-vals #(reduce-kv assoc % col-vals)]
    (map update-vals group)))

(defn aggregate-data
  [state columns aggregated-columns data]
  (let [anchors (remove (set aggregated-columns) columns)
        groups (group-by #(select-keys % anchors) data)]
    (->> groups
         (reduce-kv (fn [acc k v]
                      (assoc acc k (aggregate-groups state aggregated-columns v)))
                    {})
         (vals)
         (flatten))))

(defn- compare-records
  [order-by a b]
  (->> order-by
       (map #(fn [x y] (compare (get x %) (get y %))))
       (map #(% a b))
       vec
       (#(conj % (compare (vec a) (vec b))))
       (reduce #(if (zero? %2)
                  %1
                  (reduced %2)) 0)))

(defn- sort-result
  [order-by data]
  (if (seq order-by)
    (apply sorted-set-by (partial compare-records order-by) data)
    data))

(sch/defn transform-records :- [{sch/Keyword sch/Any}]
  "Apply a series of transformation functions to each record."
  [state :- State
   transforms :- [IFn]
   data :- [{sch/Keyword sch/Any}]]
  (if (seq transforms)
    (map (fn [r] (reduce #(%2 %1) r transforms)) data)
    data))

(def add-calculated-columns (module/build-calculated-columns :calculate))
(def add-post-aggregate-columns (module/build-calculated-columns :post-aggregate))

(defn maybe-apply
  [f data]
  (if f
    (f data)
    data))

(defn repeat-rows
  [repeat-by data]
  (if repeat-by
    (letfn [(reducer [acc d]
              (apply conj acc (repeat (or (get d repeat-by) 0) d)))]
      (reduce reducer [] data))
    data))

(sch/defn generate-report
  "Generate a report given an output configuration and data."
  [state :- State
   columns
   {:keys [rewrites pre-transforms pre-filters apply-fn
           transforms filters aggregate-on order-by function
           repeat-by identified-by]}
   data :- [{sch/Keyword sch/Any}]]
  (if function
    (function state data)
    (let [projection-columns (cond-> columns
                               (and repeat-by (not (.contains columns repeat-by)))
                               (conj repeat-by)

                               (and identified-by (not (.contains columns identified-by)))
                               (concat identified-by))]
      (->> data
           (transform-records state rewrites)
           (add-calculated-columns state projection-columns)
           (transform-records state pre-transforms)
           (filter-records state pre-filters)
           (maybe-apply apply-fn)
           (aggregate-data state projection-columns aggregate-on)
           (add-post-aggregate-columns state projection-columns)
           (transform-records state transforms)
           (filter-records state filters)
           (project projection-columns)
           (sort-result order-by)
           (repeat-rows repeat-by)))))

(defn- all-cols?
  [cols]
  (= (first cols) :all))

(defn sighting-field-map
  "Return key-value pair of sighting fields and their order weighting."
  [sf value-fn]
  (letfn [(reducer [acc k v] (assoc acc (util.sf/user-key k) (value-fn v)))]
    (reduce-kv reducer {} (group-by :sighting-field-key sf))))

(defn sighting-field-label-map
  "Return key-value pair of sighting fields and its label."
  [sf]
  (sighting-field-map sf #(first (sort (map :sighting-field-label %)))))

(defn- cons-headings
  [state sf cols cust-titles data]
  (let [sf-labels (sighting-field-label-map sf)]
    (cons (map #(or (get cust-titles %)
                    (get-in @module/known-columns [% :heading])
                    (get sf-labels %)
                    (tr/translate state
                                  (keyword (str "report/" (name %))))) cols)
          data)))

(defn- as-dashed-row
  [state cols row]
  (map #(or (get row %) "-") cols))

(defn- as-row
  [state cols row]
  (map #(get row %) cols))

(defn- as-dashed-rows
  [state cols data]
  (map (partial as-dashed-row state cols) data))

(defn- as-rows
  [state cols data]
  (let [eff-cols (if (all-cols? cols)
                   (keys (first data))
                   cols)]
    (map (partial as-row state eff-cols) data)))

(defn- to-csv-string
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (str io-str)))

(defn custom-titles
  [state title-fn]
  (if title-fn
    (title-fn state)
    {}))

(defn stylise-output
  "Update data with global stylisation changes."
  [state params cols data]
  (if (get-in params [:options :leave-blank-fields-empty])
    (as-rows state cols data)
    (as-dashed-rows state cols data)))

(defn sighting-field-columns
  "Return all sighting field columns present in the dataset."
  [sf data]
  (let [sfset (into #{} (keys sf))]
    (if (empty? sfset)
      #{}
      (reduce (fn [acc r] (set/union acc (set/intersection sfset (into #{} (keys r)))))
              #{}
              data))))

(defn sighting-field-ordering-map
  "Return key-value pair of sighting fields and their order weighting."
  [sf]
  (sighting-field-map sf #(apply min (map :sighting-field-ordering %))))

(defn expand-all-fields-col
  "Expand all-fields into an ordered list of sighting fields"
  [data sf cols]
  (let [sf-order (sighting-field-ordering-map sf)]
    (mapcat #(if (= :all-fields %)
               (sort-by (partial get sf-order)
                        (sighting-field-columns sf-order data))
               (list %))
            cols)))

(defn expand-wildcard-col
  "Expand the wildcard column into a seq of all fields."
  [cols]
  (if (all-cols? cols)
    (let [fields (->> model/extended-schema-definitions
                      (remove #(get (second %) :export-excluded))
                      (map first)
                      sort
                      vec)]
      (conj fields :all-fields))
    cols))

(defn expand-cols
  "Returns seq of the columns in the report, as per their ordering."
  [data sf cols]
  (->> cols
       expand-wildcard-col
       (expand-all-fields-col data sf)))

(defn- exportable-report
  "Generate a report as a CSV."
  [state params column-title-fn data]
  (let [sf (sighting-field/get-all state)
        cols (expand-cols data sf (:columns params))
        d (generate-report state cols params data)]
    (if (:function params)
      (to-csv-string d)
      (->> d
           (stylise-output state params cols)
           (cons-headings state sf cols (custom-titles state column-title-fn))
           (to-csv-string)))))

(sch/defn report :- [sch/Any]
  "Produce a report, with each record represented as a vector."
  [report-key :- sch/Keyword
   state :- State
   configuration
   data :- [{sch/Keyword sch/Any}]]
  (loader/load-user-modules state)
  (let [report (module/get-report state report-key)
        conf ((:output report) state configuration)
        sf (sighting-field/get-all state)
        cols (expand-cols data sf (:columns conf))]
    (->> data
         (generate-report state cols conf)
         (as-rows state cols))))

(sch/defn csv-report
  "Produce the report as a CSV."
  [report-key :- sch/Keyword
   state :- State
   configuration
   data :- [{sch/Keyword sch/Any}]]
  (let [report (module/get-report state report-key)]
    (exportable-report
     state
     ((:output report) state configuration)
     (:column-title-fn report)
     data)))

(def time-formatter (tf/formatter-local "yyyy-MM-dd_HHmm"))

(defn- content-disposition
  [report]
  (format "attachment; filename=\"%s_%s.csv\""
          (get report :file-prefix)
          (tf/unparse time-formatter (tl/local-now))))

(sch/defn export
  "Handler for an export request."
  [state :- State
   report-key :- sch/Keyword
   configuration]
  (loader/load-user-modules state)
  (if-let [report (module/get-report state report-key)]
    (let [sightings (query/get-by state (:by report))
          data (csv-report report-key state configuration sightings)]
      (-> (r/response data)
          (r/content-type "text/csv; charset=utf-8")
          (r/header "Content-Length" (count data))
          (r/header "Content-Disposition"
                    (content-disposition report))))
    (format "Report '%s' is not known" (name report-key))))

(defn ->report-descriptor
  [r rk]
  (assoc (select-keys r [:title :form :description])
         :report-key (name rk)))

(defn- report-configuration-reducer
  [acc k v]
  (conj acc (->report-descriptor v k)))

(sch/defn refresh-reports
  "Rediscover and evaluate report modules."
  [state]
  (loader/load-user-modules state))

(sch/defn available-reports
  "Map of all available reports."
  [state]
  (loader/load-user-modules state)
  (reduce-kv report-configuration-reducer [] (module/all-reports state)))

(sch/defn get-configuration
  "Configuration of the given report."
  [state report-key]
  (loader/load-user-modules state)
  (let [r (get (module/all-reports state) report-key)]
    (->report-descriptor r report-key)))
