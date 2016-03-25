(ns ctdp.album
  (:require [clj-time.core :as t]
            [schema.core :as s]
            [ctdp.model.album :as ma]
            [ctdp.photo :as photo]))

(s/defn exceed-ir-threshold :- s/Bool
  [config photos]
  (let [nightfn (partial photo/night? (:night-start-hour config) (:night-end-hour config))
        ir-check-fn (partial photo/infrared-sane? nightfn
                             (:infrared-iso-value-threshold config))
        ir-failed (count (remove identity (map ir-check-fn photos)))
        night-total (count (filter #(nightfn (t/hour (:datetime %))) photos))]
    (if (not (zero? night-total))
      (> (/ ir-failed night-total) (:erroneous-infrared-threshold config))
      false)))

(s/defn list-problems :- [s/Keyword]
  [config album-data]
  (if (exceed-ir-threshold config (map (fn [[k v]] v) album-data))
    [:datetime]
    []))

(s/defn extract-date
  [cmp album]
  (:datetime (first (sort #(cmp (:datetime %1) (:datetime %2)) album))))

(defn- extract-start-date
  []
  (partial extract-date t/before?))

(defn- extract-end-date
  []
  (partial extract-date t/after?))

(defn- extract-make
  [album]
  (:make (:camera (first album))))

(defn- extract-model
  [album]
  (:model (:camera (first album))))

(s/defn extract-metadata :- ma/ExtractedMetadata
  [album]
  {:datetime-start ((extract-start-date) album)
   :datetime-end ((extract-end-date) album)
   :make (extract-make album)
   :model (extract-model album)})

(s/defn album :- ma/Album
  [state set-data]
  (let [album-data (into {} (map (fn [[k v]] [k (photo/normalise v)]) set-data))]
    {:photos album-data
     :metadata (extract-metadata (vals album-data))
     :problems (list-problems (:config state) album-data)}))

(s/defn album-set
  [state tree-data]
  (into {} (map (fn [[k v]] (vector k (album state v))) tree-data)))
