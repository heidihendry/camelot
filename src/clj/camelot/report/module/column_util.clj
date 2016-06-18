(ns camelot.report.module.column-util
  (:require [camelot.report.sighting-independence :as indep]
            [clj-time.core :as t]))

(defn aggregate-numeric
  [group-col col data]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       (flatten)
       (reduce #(if (nil? %2)
                  %1
                  (+ %1 %2)) 0)))

(defn ->percentage
  [{:keys [n d]}]
  (format "%.2f" (* 100 (float (/ n d)))))

(defn aggregate-boolean
  [group-col col data]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       (flatten)
       (reduce #(cond
                  (= %2 "X") (update (update %1 :n inc) :d inc)
                  (= %2 "") (update %1 :d inc)
                  (nil? %2) %1)
               {:n 0 :d 0})
       (->percentage)))

(defn aggregate-by-species
  [col data]
  (aggregate-numeric :taxonomy-id col data))

;; TODO migrate everything off of this
(defn aggregate-by-trap-station
  [col data]
  (aggregate-numeric :trap-station-session-id col data))

(defn aggregate-by-trap-station-session
  [col data]
  (aggregate-numeric :trap-station-session-id col data))

(defn aggregate-with-reducer
  [pred group-col col data]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       (flatten)
       (reduce pred nil)))

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
  (let [obs-reducer (partial independent-observation-reducer state)]
    (->> data
         (group-by :trap-station-session-id)
         (reduce-kv obs-reducer {}))))

(defn calculate-independent-observations
  [state data]
  (let [all-spp-obs (get-independent-observations state data)
        path #(vector (:trap-station-session-id %) (:taxonomy-id %))
        get-obs #(get-in all-spp-obs (path %))]
    (map #(assoc % :independent-observations (or (get-obs %) 0)) data)))

(defn- get-nights-for-sample
  [sample]
  (let [start (:trap-station-session-start-date sample)
        end (:trap-station-session-end-date sample)]
    (t/in-days (t/interval (t/floor start t/day)
                           (t/floor end t/day)))))

(defn- trap-session-nights-reducer
  [acc k v]
  (assoc acc k (get-nights-for-sample (first v))))

(defn- get-nights-for-sessions
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (reduce-kv trap-session-nights-reducer {})))

(defn calculate-nights-elapsed
  [state data]
  (let [nights (get-nights-for-sessions data)]
    (map #(assoc % :nights-elapsed
                 (get nights (:trap-station-session-id %))) data)))

(defn column-from-existing
  [col newcol state data]
  (map #(assoc % newcol (get % col))) data)

(defn calculate-total-nights
  [state data]
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

(defn calculate-count
  [tbl state data]
  (map (partial assoc-count tbl) data))
