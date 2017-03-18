(ns camelot.report.core
  "Generate a report from a DSL."
  (:require
   [camelot.util.db :as db]
   [camelot.system.state :refer [State]]
   [camelot.report.module.loader :as loader]
   [camelot.report.module.core :as module]
   [camelot.translation.core :as tr]
   [camelot.model.camera :as camera]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.sighting :as sighting]
   [camelot.model.site :as site]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.species-mass :as species-mass]
   [camelot.model.survey :as survey]
   [camelot.model.survey-site :as survey-site]
   [camelot.model.trap-station :as trap-station]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [clj-time.local :as tl]
   [clj-time.format :as tf]
   [clojure.data.csv :as csv]
   [clojure.set :as set]
   [ring.util.response :as r]
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.model :as model])
  (:import
   (clojure.lang IFn)))

(def data-definitions
  {:media-id [media/get-all* [:trap-station-session-camera-id] [:sighting-id :photo-id]]
   :sighting-id [sighting/get-all* [:taxonomy-id] [:media-id]]
   :taxonomy-id [taxonomy/get-all [:species-mass-id] [:sighting-id]]
   :photo-id [photo/get-all* [] [:media-id]]
   :trap-station-session-camera-id [trap-station-session-camera/get-all* [:trap-station-session-id :camera-id] [:media-id]]
   :camera-id [camera/get-all [:camera-status-id] [:trap-station-session-camera-id]]
   :trap-station-session-id [trap-station-session/get-all* [:trap-station-id] [:trap-station-session-camera-id]]
   :trap-station-id [trap-station/get-all* [:survey-site-id] [:trap-station-session-id]]
   :survey-site-id [survey-site/get-all* [:survey-id :site-id] []]
   :species-mass-id [species-mass/get-all [] [:taxonomy-id]]
   :camera-status-id [camera-status/get-all [] [:camera-id]]
   :site-id [site/get-all [] [:survey-site-id :trap-station-id]]
   :survey-id [survey/get-all [] [:survey-site-id]]})

(defn known-dep?
  [acc dep]
  (some? (some #{(first dep)} (map first acc))))

(defn resolution-order
  [data by]
  ((fn [acc deps rdeps]
     (if (empty? deps)
       (if (empty? rdeps)
         acc
         (let [cd (get data (ffirst rdeps))]
           (recur (conj acc (first rdeps))
                  (let [ndeps (filter #(not (known-dep? acc %))
                                      (mapv #(vector % %) (second cd)))]
                    (if (seq ndeps)
                      (concat deps ndeps)
                      deps))
                  (let [nrdeps (filter #(not (known-dep? acc %))
                                       (mapv #(vector % (ffirst rdeps)) (nth cd 2)))]
                    (if (seq nrdeps)
                      (concat (rest rdeps) nrdeps)
                      (rest rdeps))))))
       (let [cd (get data (ffirst deps))]
         (recur (conj acc (first deps))
                (let [ndeps (filter #(not (known-dep? acc %))
                                    (mapv #(vector % %) (second cd)))]
                    (if (seq ndeps)
                      (concat (rest deps) ndeps)
                      (rest deps)))
                (let [nrdeps (filter #(not (known-dep? acc %))
                                     (mapv #(vector % (ffirst deps)) (nth cd 2)))]
                    (if (seq nrdeps)
                      (concat rdeps nrdeps)
                      rdeps))))))
   [] [[by by]] []))

(defn- build-records
  [rorder data]
  (clojure.tools.logging/error data)
  (reduce (fn [acc [tbl key]]
              (mapcat (fn [a] (let [rs (get-in data [tbl key (get a key)])]
                                (if (seq rs)
                                  (map #(merge a %) rs)
                                  (list a))))
                      acc))
            (map #(into {} %) (apply concat (vals (get-in data (first rorder)))))
            (rest rorder)))

(defn- join-all
  [state by]
  (let [rorder (resolution-order data-definitions by)
        data (reduce (fn [acc [t1 [t2s]]]
                       (let [qres ((first (get data-definitions t1)) state)]
                         (assoc acc t1
                                (reduce (fn [iacc t2] (assoc iacc t2 (group-by t2 qres)))
                                        {}
                                        t2s))))
                     {} (group-by first rorder))]
    (build-records rorder data)))

(s/defn get-by :- [{s/Keyword s/Any}]
  "Retrieve the data for the given report type."
  [state :- State
   by :- s/Keyword]
  (join-all state (if (= by :all)
                    :media-id
                    (keyword (str (name by) "-id")))))

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

(defn- better-for-known-keys
  [smaller larger]
  (when (= smaller (select-keys larger (keys smaller)))
    larger))

(defn- distinct-in-results
  [results]
  (loop [{:keys [acc check against] :as ps} {:acc [] :against {} :check results}]
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

(defn- aggregate-reducer
  [state group acc c]
  (let [f (get-in @module/known-columns [c :aggregate])]
    (if f
      (assoc acc c (f state c group))
      acc)))

(defn- aggregate-groups
  [state aggregated-columns group]
  (let [col-vals (reduce (partial aggregate-reducer state group) {} aggregated-columns)
        update-vals #(reduce-kv (fn [acc col v]
                                  (if (nil? (acc col))
                                    acc
                                    (assoc acc col v)))
                                % col-vals)]
    (map update-vals group)))

(defn- aggregate-data
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

(s/defn transform-records :- [{s/Keyword s/Any}]
  "Apply a series of transformation functions to each record."
  [state :- State
   transforms :- [IFn]
   data :- [{s/Keyword s/Any}]]
  (if (seq transforms)
    (map (fn [r] (reduce #(%2 %1) r transforms)) data)
    data))

(def add-calculated-columns (module/build-calculated-columns :calculate))
(def add-post-aggregate-columns (module/build-calculated-columns :post-aggregate))

(defn maybe-apply
  [state f data]
  (if f
    (f state data)
    data))

(s/defn generate-report
  "Generate a report given an output configuration and data."
  [state :- State
   {:keys [columns rewrites pre-transforms pre-filters apply-fn
           transforms filters aggregate-on order-by function]}
   data :- [{s/Keyword s/Any}]]
  (if function
    (function state data)
    (->> data
         (transform-records state rewrites)
         (add-calculated-columns state columns)
         (transform-records state pre-transforms)
         (filter-records state pre-filters)
         (maybe-apply state apply-fn)
         (aggregate-data state columns aggregate-on)
         (add-post-aggregate-columns state columns)
         (transform-records state transforms)
         (filter-records state filters)
         (project columns)
         (sort-result order-by))))

(defn- all-cols?
  [cols]
  (= (first cols) :all))

(defn- cons-headings
  [state columns cust-titles data]
  (let [cols (if (all-cols? columns)
               (keys (first data))
               columns)]
    (cons (map #(or (get cust-titles %)
                    (get-in @module/known-columns [% :heading])
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

(defn- exportable-report
  "Generate a report as a CSV."
  [state params column-title-fn data]
  (let [d (generate-report state params data)
        cols (if (all-cols? (:columns params))
               (map first (remove #(get (second %) :export-excluded)
                                  model/extended-schema-definitions))
               (:columns params))]
    (if (:function params)
      (to-csv-string d)
      (->> d
           (stylise-output state params cols)
           (cons-headings state cols (custom-titles state column-title-fn))
           (to-csv-string)))))

(s/defn report :- [s/Any]
  "Produce a report, with each record represented as a vector."
  [report-key :- s/Keyword
   state :- State
   configuration
   data :- [{s/Keyword s/Any}]]
  (loader/load-user-modules state)
  (let [report (module/get-report state report-key)
        conf ((:output report) state configuration)]
    (->> data
         (generate-report state conf)
         (as-rows state (:columns conf)))))

(s/defn csv-report
  "Produce the report as a CSV."
  [report-key :- s/Keyword
   state :- State
   configuration
   data :- [{s/Keyword s/Any}]]
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

(s/defn export
  "Handler for an export request."
  [state :- State
   report-key :- s/Keyword
   configuration]
  (loader/load-user-modules state)
  (if-let [report (module/get-report state report-key)]
    (let [sightings (get-by state (:by report))
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

(s/defn refresh-reports
  "Rediscover and evaluate report modules."
  [state]
  (loader/load-user-modules state))

(s/defn available-reports
  "Map of all available reports."
  [state]
  (loader/load-user-modules state)
  (reduce-kv report-configuration-reducer [] (module/all-reports state)))

(s/defn get-configuration
  "Configuration of the given report."
  [state report-key]
  (loader/load-user-modules state)
  (let [r (get (module/all-reports state) report-key)]
    (->report-descriptor r report-key)))
