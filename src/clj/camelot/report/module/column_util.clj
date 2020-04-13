(ns camelot.report.module.column-util
  "Utilities for defining report columns."
  (:require
   [camelot.report.sighting-independence :as indep]
   [camelot.model.survey :as survey]
   [clj-time.core :as t]
   [schema.core :as sch]
   [camelot.spec.schema.state :refer [State]])
  (:import
   (clojure.lang IFn)))

(sch/defn aggregate-numeric :- sch/Num
  "Aggregate numeric values of `col' by summation."
  [group-col :- sch/Keyword
   state :- State
   col :- sch/Keyword
   data :- [{sch/Keyword sch/Any}]]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       (flatten)
       (reduce #(if (nil? %2)
                  %1
                  (+ %1 %2)) 0)))

(defn- ->percentage
  [{:keys [n d]}]
  (when-not (zero? d)
    (format "%.2f" (* 100 (float (/ n d))))))

(defn- boolean-reducer
  [col acc v]
  (cond
    (= (get v col) "X") (update (update acc :n inc) :d inc)
    (= (get v col) "") (update acc :d inc)
    (nil? (get v col)) acc))

(defn- boolean-sighting-reducer
  [col acc v]
  (cond
    (= (get v col) "X") (assoc acc
                               :n (+ (or (:sighting-quantity v) 0) (:n acc))
                               :d (+ (or (:sighting-quantity v) 0) (:d acc)))
    (= (get v col) "") (assoc acc :d (+ (or (:sighting-quantity v) 0) (:d acc)))
    (nil? (get v col)) acc))

(defn- aggregate-boolean*
  [reducer state col data]
  (->> data
       (reduce (partial reducer col) {:n 0 :d 0})
       (->percentage)))

(sch/defn aggregate-boolean
  "Aggregate boolean fields as a percentage of records."
  ([state :- State
    col :- sch/Keyword
    data :- [{sch/Keyword sch/Any}]]
   (aggregate-boolean* boolean-reducer state col data))
  ([group-col :- sch/Keyword
    state :- State
    col :- sch/Keyword
    data :- [{sch/Keyword sch/Any}]]
   (aggregate-boolean state col data)))

(sch/defn aggregate-boolean-by-independent-observations
  "Aggregate boolean fields as a percentage of sighting quantities."
  ([state :- State
    col :- sch/Keyword
    data :- [{sch/Keyword sch/Any}]]
   (aggregate-boolean* boolean-sighting-reducer state col data))
  ([group-col :- sch/Keyword
    state :- State
    col :- sch/Keyword
    data :- [{sch/Keyword sch/Any}]]
   (aggregate-boolean-by-independent-observations state col data)))

(sch/defn aggregate-by-species
  "Numeric aggregation by species."
  [state :- State
   col :- sch/Keyword
   data :- [{sch/Keyword sch/Any}]]
  (aggregate-numeric :taxonomy-id state col data))

(sch/defn aggregate-by-trap-station-session
  "Numeric aggregation by trap session."
  [state :- State
   col :- sch/Keyword
   data :- [{sch/Keyword sch/Any}]]
  (aggregate-numeric :trap-station-session-id state col data))

(sch/defn aggregate-with-reducer
  [reducer :- IFn
   group-col :- sch/Keyword
   state :- State
   col :- sch/Keyword
   data :- [{sch/Keyword sch/Any}]]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       flatten
       (reduce reducer nil)))

(defn- species-sighting-reducer
  [acc v]
  (let [spp (:species-id v)
        qty (:count v)]
    (assoc acc spp (+ (or (get acc spp) 0) qty))))

(defn- species-sightings
  [state v]
  (->> v
       (filter :taxonomy-id)
       (filter :media-capture-timestamp)
       (indep/extract-independent-sightings state)
       (flatten)
       (reduce species-sighting-reducer {})))

(defn- independent-observation-reducer
  [state acc k v]
  (->> v
       (species-sightings state)
       (assoc acc k)))

(defn- get-independent-observations
  [state data]
  (survey/with-survey-settings [s state]
    (let [obs-reducer (partial independent-observation-reducer s)]
      (->> data
           (group-by :trap-station-session-id)
           (reduce-kv obs-reducer {})))))

(sch/defn calculate-independent-observations
  "Return the number of independent observations for a species"
  [state :- State
   data :- [{sch/Keyword sch/Any}]]
  (let [all-spp-obs (get-independent-observations state data)
        path #(vector (:trap-station-session-id %) (:taxonomy-id %))
        get-obs #(get-in all-spp-obs (path %))]
    (map #(assoc % :independent-observations (or (get-obs %) 0)) data)))

(defn- get-nights-for-sample
  [sample]
  (let [start (:trap-station-session-start-date sample)
        end (:trap-station-session-end-date sample)]
    (if (or (nil? start)
            (nil? end))
      0
      (t/in-days (t/interval (t/floor start t/day)
                             (t/floor end t/day))))))

(defn- trap-session-nights-reducer
  [acc k v]
  (assoc acc k (if (seq (remove #(:trap-station-session-camera-media-unrecoverable %) v))
                 (get-nights-for-sample (first v))
                 0)))

(defn- get-nights-for-sessions
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (reduce-kv trap-session-nights-reducer {})))

(sch/defn calculate-nights-elapsed
  "Assoc the number of nights elapsed where at least one camera was active."
  [state :- State
   data :- [{sch/Keyword sch/Any}]]
  (let [nights (get-nights-for-sessions data)]
    (map #(assoc % :nights-elapsed
                 (get nights (:trap-station-session-id %))) data)))

(sch/defn calculate-total-nights
  "Assoc the total number of nights cameras were active. Multiple active
  cameras on one night all contribute to the total."
  [state :- State
   data :- [{sch/Keyword sch/Any}]]
  (let [nights (get-nights-for-sessions data)
        v (reduce + 0 (vals nights))]
    (map #(assoc % :total-nights v) data)))

(defn- assoc-count
  [tbl data]
  (let [cnt (keyword (str (name tbl) "-count"))
        id (keyword (str (name tbl) "-id"))]
    (assoc data cnt
           (if (get data id)
             1
             0))))

(sch/defn calculate-count
  "Assoc the count of records for a `table'.
A table corresponds to any `-id' column, minus the '-id' suffix."
  [tbl :- sch/Keyword
   state :- State
   data :- [{sch/Keyword sch/Any}]]
  (map (partial assoc-count tbl) data))
