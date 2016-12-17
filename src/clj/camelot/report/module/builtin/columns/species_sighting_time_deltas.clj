(ns camelot.report.module.builtin.columns.species-sighting-time-deltas
  (:require
   [camelot.report.module.core :as module]
   [clj-time.core :as t]))

(defn- get-nights-per-independent-observation
  [record]
  (let [obs (:independent-observations record)
        nights (or (:total-nights record) (:nights-elapsed record))]
    (cond
        (nil? obs) nil
        (or (nil? nights) (zero? nights)) "-"
        :else (format "%.3f" (* 100 (double (/ obs nights)))))))

(defn sighting-time-delta-reducer
  [acc v]
  (if (and (:trap-station-id v) (:taxonomy-id v))
    (let [lt (vector (:trap-station-id v) (:taxonomy-id v))
          last (or (get-in acc [:state lt]) nil)
          dt (when last
               (t/interval last (:media-capture-timestamp v)))]
      (assoc-in
       (update acc :data #(conj % (assoc v :sighting-time-delta dt)))
       [:state lt] (:media-capture-timestamp v)))
    acc))

(defn to-1dp
  [v]
  (format "%.1f" v))

(defn calculate-sighting-time-delta
  [state data]
  (if (some :sighting-time-delta data)
    data
    (:data (reduce sighting-time-delta-reducer {:data []}
                   (sort-by :media-capture-timestamp data)))))

(defn calculate-time-delta-in-seconds
  [state data]
  (->> data
       (calculate-sighting-time-delta state)
       (map (fn [d]
              (assoc d :sighting-time-delta-seconds
                     (or (some-> d :sighting-time-delta t/in-seconds str)
                         "0"))))))

(defn calculate-time-delta-in-minutes
  [state data]
  (->> data
       (calculate-sighting-time-delta data)
       (map (fn [d]
              (assoc d :sighting-time-delta-minutes
                     (or (some-> d :sighting-time-delta t/in-minutes str)
                         "0"))))))

(defn calculate-time-delta-in-hours
  [state data]
  (->> data
       (calculate-sighting-time-delta data)
       (map (fn [d] (assoc d :sighting-time-delta-hours
                           (or (some-> d
                                       :sighting-time-delta
                                       t/in-minutes
                                       (/ (float 60))
                                       to-1dp)
                               "0.0"))))))

(defn calculate-time-delta-in-days
  [state data]
  (->> data
       (calculate-sighting-time-delta data)
       (map (fn [d]
              (assoc d :sighting-time-delta-days
                     (or (some-> d
                                 :sighting-time-delta
                                 t/in-minutes
                                 (/ (float 60) 24)
                                 to-1dp)
                         "0.0"))))))

(module/register-column
 :sighting-time-delta-seconds
 {:post-aggregate calculate-time-delta-in-seconds})

(module/register-column
 :sighting-time-delta-minutes
 {:post-aggregate calculate-time-delta-in-minutes})

(module/register-column
 :sighting-time-delta-hours
 {:post-aggregate calculate-time-delta-in-hours})

(module/register-column
 :sighting-time-delta-days
 {:post-aggregate calculate-time-delta-in-days})
