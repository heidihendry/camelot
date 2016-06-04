(ns camelot.report-builder
  (:require [camelot.db :as db]
            [yesql.core :as sql]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.handler.albums :as albums]
            [camelot.processing.album :as album]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [clojure.set :as set]
            [clj-time.core :as t]
            [camelot.util.report :as report-util]
            [ring.util.response :as r]
            [camelot.translation.core :as tr]))

(sql/defqueries "sql/reports.sql" {:connection db/spec})

(defn get-all-data-by-survey
  [state]
  (db/clj-keys (-get-all-data-by-survey)))

(defn get-all-data-by-species
  [state]
  (db/clj-keys (-get-all-data-by-species)))

(defn get-all-data-by-site
  [state]
  (db/clj-keys (-get-all-data-by-site)))

(defn get-all-data-by-camera
  [state]
  (db/clj-keys (-get-all-data-by-camera)))

(def data-by {
              :survey get-all-data-by-survey
              :species get-all-data-by-species
              :site get-all-data-by-site
              :camera get-all-data-by-camera})

(defn get-data-by
  [state by]
  ((get data-by by) state))

(defn- species-sighting-reducer
  [acc v]
  (let [spp (:species v)
        qty (:count v)]
    (assoc acc spp (+ (or (get acc spp) 0) qty))))

(defn- get-independent-observations
  [state data]
  (->> data
       (group-by :trap-station-session-id)
       (reduce-kv (fn [acc k v]
                    (assoc acc k
                           (->> v
                                (remove (fn [x] (nil? (:species-scientific-name x))))
                                (remove (fn [x] (nil? (:media-capture-timestamp x))))
                                (album/extract-independent-sightings state)
                                (flatten)
                                (reduce species-sighting-reducer {}))))
                  {})))

(defn- calculate-independent-observations
  [state data]
  (let [spp-obs (get-independent-observations state data)]
    (map #(assoc % :independent-observations
                 (or (get-in spp-obs [(:trap-station-session-id %)
                                      (:species-scientific-name %)]) 0)) data)))

(defn- get-nights-for-sample
  [sample]
  (let [start (:trap-station-session-start-date sample)
        end (:trap-station-session-end-date sample)]
    (t/in-days (t/interval start end))))

(defn- get-nights-for-sessions
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (reduce-kv (fn [acc k v]
                    (assoc acc k (get-nights-for-sample (first v))))
                  {})))

(defn- calculate-nights-elapsed
  [state data]
  (let [nights (get-nights-for-sessions data)]
    (map #(assoc % :nights-elapsed (get nights (:trap-station-session-id %))) data)))

(defn- get-nights-per-independent-observation
  [record]
  (let [obs (:independent-observations record)
        nights (:nights-elapsed record)]
    (if (or (nil? nights) (zero? nights))
      "-"
      (format "%.3f" (* 100 (double (/ obs nights)))))))

(defn sum-group
  [group-col col data]
  (reduce + 0
          (flatten (map
                    (fn [d] (get (first d) col))
                    (vals (group-by group-col data))))))

(defn night-group-value-for
  [col data]
  (reduce + 0
          (flatten (map
                    (fn [d] (get (first d) col))
                    (vals (group-by :trap-station-session-id data))))))

(defn indep-obs-group-value-for
  [col data]
  (reduce + 0
          (flatten (map
                    (fn [d] (get (first d) col))
                    (vals (group-by :trap-station-session-id data))))))

(defn presense-absence-group-value-for
  [col data]
  (let [obs (indep-obs-group-value-for :independent-observations data)]
    (if (zero? obs) "" "X")))

(defn- calculate-independent-observations-per-night
  [state data]
  (->> data
       (map #(assoc % :independent-observations-per-night
                    (get-nights-per-independent-observation %)))))

(defn- calculate-presence-absence
  [state data]
  (->> data
       (calculate-independent-observations state)
       (map #(assoc % :presence-absence
                    (if (= (:independent-observations %) 0)
                      ""
                      "X")))))

(defn- calculate-media-count
  [state data]
  (->> data
       (map #(assoc % :media-count
                    (if (nil? (:media-id %))
                      0
                      1)))))

(defn- calculate-trap-station-session-camera-count
  [state data]
  (->> data
       (map #(assoc % :trap-station-session-camera-count
                    (if (nil? (:trap-station-session-camera-id %))
                      0
                      1)))))

(defn- calculate-trap-station-session-count
  [state data]
  (->> data
       (map #(assoc % :trap-station-session-count
                    (if (nil? (:trap-station-session-id %))
                      0
                      1)))))

(defn- calculate-trap-station-count
  [state data]
  (->> data
       (map #(assoc % :trap-station-count
                    (if (nil? (:trap-station-id %))
                      0
                      1)))))

(def calculated-columns
  {:independent-observations {:calculate calculate-independent-observations
                              :aggregated-column indep-obs-group-value-for}
   :nights-elapsed {:calculate calculate-nights-elapsed
                    :aggregated-column night-group-value-for}
   :presence-absence {:calculate calculate-presence-absence
                      :aggregated-column presense-absence-group-value-for}
   :media-count {:calculate calculate-media-count
                 :aggregated-column (partial sum-group :media-id)}
   :trap-station-count {:calculate calculate-trap-station-count
                        :aggregated-column (partial sum-group :trap-station-id)}
   :trap-station-session-count {:calculate calculate-trap-station-session-count
                                :aggregated-column (partial sum-group :trap-session-count-id)}
   :trap-station-session-camera-count {:calculate calculate-trap-station-session-camera-count
                                       :aggregated-column
                                       (partial sum-group :trap-station-session-camera-id)}
   :independent-observations-per-night {:post-aggregate calculate-independent-observations-per-night}
   })

(defn build-calculated-columns
  [t]
  (fn [state columns data]
    (let [cols (filter (set (keys calculated-columns)) columns)]
      (reduce (fn [acc c] (let [f (get-in calculated-columns [c t])]
                            (if f
                              (f state acc)
                              acc)))
              data cols))))

(def add-calculated-columns (build-calculated-columns :calculate))
(def add-post-aggregate-columns (build-calculated-columns :post-aggregate))

(defn fill-keys
  [columns data]
  (map
   #(reduce (fn [acc c]
              (if (contains? acc c)
                acc
                (assoc acc c nil)))
            %
            columns)
   data))

(defn distinct-for-known-keys
  [test-rec comp-rec]
  (let [ks (keys test-rec)]
    (if (= test-rec comp-rec)
      true
      (if (every? #(= (test-rec %) (comp-rec %)) ks)
        false
        true))))

(defn distinct-in-results
  [result-set testrec]
  (every? #(distinct-for-known-keys testrec %) result-set))

(defn project
  [columns data]
  (let [result (set/project data columns)]
    (into #{} (fill-keys columns
                         (filter #(distinct-in-results result %) result)))))

(defn filter-records
  [state filters data]
  (filter (fn [r] (if (seq filters)
                    (every? #(% r) filters)
                    true))
          data))

(defn aggregate-groups
  [aggregated-columns group]
  (let [col-vals (reduce #(let [f (get-in calculated-columns [%2 :aggregated-column])]
                            (if f
                              (assoc %1 %2 (f %2 group))
                              %1))
                         {}
                         aggregated-columns)]
    (map
     #(reduce-kv (fn [acc col v] (assoc acc col v)) % col-vals)
     group)))

(defn aggregate-data
  [columns aggregated-columns data]
  (let [anchors (remove (set aggregated-columns) columns)
        groups (group-by #(select-keys % anchors) data)]
    (flatten
     (vals
      (reduce-kv (fn [acc k v] (assoc acc k (aggregate-groups aggregated-columns v)))
                 {}
                 groups)))))

(defn- sort-result
  [order-by data]
  (if (seq order-by)
    (apply sorted-set-by (fn [a b]
                     (->> order-by
                          (map #(fn [x y] (compare (get x %) (get y %))))
                          (map #(% a b))
                          (cons (compare (into [] a) (into [] b)))
                          (reduce #(if (zero? %2)
                                     %1
                                     (reduced %2)) 0)))
                   data)
    data))

(defn- transform-records
  [state transforms data]
  (map
   (fn [r] (reduce #(%2 %1) r transforms))
   data))

(defn report
  [state {:keys [columns filters transforms aggregate-on order-by]} data]
  (->> data
       (add-calculated-columns state columns)
       (aggregate-data columns aggregate-on)
       (add-post-aggregate-columns state columns)
       (transform-records state transforms)
       (filter-records state filters)
       (project columns)
       (sort-result order-by)))

(defn cons-headings
  [state columns data]
  (cons (map #(tr/translate (:config state) (keyword (str "report/" (name %)))) columns)
        data))

(defn- as-row
  [state cols row]
  (map #(get row %) cols))

(defn as-rows
  [state params data]
  (let [cols (:columns params)]
    (map (partial as-row state cols) data)))

(defn exportable-report
  [state params data]
  (->> data
       (report state params)
       (as-rows state params)
       (cons-headings state (:columns params))
       (report-util/to-csv-string)))
