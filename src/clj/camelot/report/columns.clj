(ns camelot.report.columns
  (:require [camelot.processing.album :as album]
            [clj-time.core :as t]))

(defn- species-sighting-reducer
  [acc v]
  (let [spp (:species v)
        qty (:count v)]
    (assoc acc spp (+ (or (get acc spp) 0) qty))))

(defn- species-sightings
  [state v]
  (->> v
       (filter :species-scientific-name)
       (filter :media-capture-timestamp)
       (album/extract-independent-sightings state)
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

(defn- get-nights-for-sample
  [sample]
  (let [start (:trap-station-session-start-date sample)
        end (:trap-station-session-end-date sample)]
    (t/in-days (t/interval start end))))

(defn- trap-session-nights-reducer
  [acc k v]
  (assoc acc k (get-nights-for-sample (first v))))

(defn- get-nights-for-sessions
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (reduce-kv trap-session-nights-reducer {})))

(defn- get-nights-per-independent-observation
  [record]
  (let [obs (:independent-observations record)
        nights (:nights-elapsed record)]
    (cond
        (nil? obs) nil
        (or (nil? nights) (zero? nights)) "-"
        :else (format "%.3f" (* 100 (double (/ obs nights)))))))

(defn- aggregate-numeric
  [group-col col data]
  (->> data
       (group-by group-col)
       (vals)
       (map #(get (first %) col))
       (flatten)
       (reduce #(if (nil? %2)
                  %1
                  (+ %1 %2)) 0)))

(defn- aggregate-by-trap-station
  [col data]
  (aggregate-numeric :trap-station-session-id col data))

(defn- presense-flag
  [v]
  (if (zero? v)
    ""
    "X"))

(defn- aggregate-presense-absence
  [col data]
  (->> data
       (aggregate-by-trap-station :independent-observations)
       (presense-flag)))

(defn- calculate-independent-observations
  [state data]
  (let [all-spp-obs (get-independent-observations state data)
        path #(vector (:trap-station-session-id %) (:species-scientific-name %))
        get-obs #(get-in all-spp-obs (path %))]
    (map #(assoc % :independent-observations (or (get-obs %) 0)) data)))

(defn- calculate-nights-elapsed
  [state data]
  (let [nights (get-nights-for-sessions data)]
    (map #(assoc % :nights-elapsed
                 (get nights (:trap-station-session-id %))) data)))

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
                    (presense-flag (:independent-observations %))))))

(defn- assoc-count
  [tbl data]
  (let [cnt (keyword (str (name tbl) "-count"))
        id (keyword (str (name tbl) "-id"))]
    (assoc data cnt
           (if (get data id)
             1
             0))))

(defn- calculate-count
  [tbl state data]
  (map (partial assoc-count tbl) data))

(def calculated-columns
  {:independent-observations
   {:calculate calculate-independent-observations
    :aggregate aggregate-by-trap-station}

   :nights-elapsed
   {:calculate calculate-nights-elapsed
    :aggregate aggregate-by-trap-station}

   :presence-absence
   {:calculate calculate-presence-absence
    :aggregate aggregate-presense-absence}

   :media-count
   {:calculate (partial calculate-count :media)
    :aggregate (partial aggregate-numeric :media-id)}

   :trap-station-count
   {:calculate (partial calculate-count :trap-station)
    :aggregate (partial aggregate-numeric :trap-station-id)}

   :trap-station-session-count
   {:calculate (partial calculate-count :trap-station-session)
    :aggregate (partial aggregate-numeric :trap-station-session-id)}

   :trap-station-session-camera-count
   {:calculate (partial calculate-count :trap-station-session-camera)
    :aggregate (partial aggregate-numeric :trap-station-session-camera-id)}

   :independent-observations-per-night
   {:post-aggregate calculate-independent-observations-per-night}})

(defn- calculate-column
  [state t acc c]
  (let [f (get-in calculated-columns [c t])]
    (if f
      (f state acc)
      acc)))

(defn build-calculated-columns
  [t]
  (fn [state columns data]
    (let [cols (filter (set (keys calculated-columns)) columns)]
      (reduce (partial calculate-column state t) data cols))))
