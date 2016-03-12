(ns ctdp.album
  (:require [clj-time.core :as t]
            [schema.core :as s]
            [ctdp.photo :as photo]
            [cats.monad.either :as either]))

(defn- night?
  [night-start night-end hour]
  (or (> hour night-start) (< hour night-end)))

(defn- infrared-sane?
  [nightfn isothresh photo]
  (let [hour (t/hour (:datetime photo))
        iso (:iso (:settings photo))
        ir? (> iso isothresh)]
    (or ir? (not (nightfn hour)))))

(defn exceed-ir-threshold
  [config photos]
  (let [isothresh (:infrared-iso-value-threshold config)
        nightfn (partial night? (:night-start-hour config) (:night-end-hour config))
        ir-fn (partial infrared-sane? nightfn isothresh)
        ir-check (map ir-fn photos)
        ir-failed (count (remove identity ir-check))
        night-total (count (filter #(nightfn (t/hour (:datetime %))) photos))]
    (when (not (zero? night-total))
      (> (/ ir-failed night-total) (:erroneous-infrared-threshold config)))))

(s/defn album
  [state set-data]
  (let [album-data (into {} (map (fn [[k v]] [k (photo/normalise v)]) set-data))
        photos (map (fn [[k v]] v) album-data)]
    (if (exceed-ir-threshold (:config state) photos)
      {:photos album-data :problems [:datetime]}
      {:photos album-data})))
