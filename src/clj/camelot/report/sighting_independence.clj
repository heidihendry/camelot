(ns camelot.report.sighting-independence
  (:require [clj-time.core :as t]
            [schema.core :as s]))

(defn- add-sighting
  "Add a new (i.e., independent) sighting."
  [state previous-sightings this-sighting]
  (let [duration (:sighting-independence-minutes-threshold (:config state))]
    (conj previous-sightings (assoc this-sighting
                                    :sighting-independence-window-end
                                    (t/plus (:media-capture-timestamp this-sighting)
                                            (t/minutes duration))))))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [previous-sightings sighting quantity]
  (let [new-qty (max (or (get sighting :sighting-quantity) 0) quantity)]
    (conj previous-sightings
          (assoc sighting :sighting-quantity new-qty))))

(defn- dependent-sighting?
  "Predicate for whether the sighting would be dependent for a timespan."
  [sighting timespan]
  (or (= sighting (:media-capture-timestamp timespan))
      (and (t/after? sighting (:media-capture-timestamp timespan))
           (t/before? sighting (:sighting-independence-window-end timespan)))))

(defn- dependent-sighting
  "Return the first dependent sighting, if any."
  [sighting datespans]
  (first (filter (partial dependent-sighting? sighting) datespans)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state acc this-sighting]
  (let [datetime (:media-capture-timestamp this-sighting)
        species (:taxonomy-id this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))
        qty (or (:sighting-quantity this-sighting) 0)
        known-sightings (get acc species)]
    (assoc acc species
           (if previous-sighting
             (update-sighting (remove #(= previous-sighting %) known-sightings)
                              previous-sighting qty)
             (add-sighting state known-sightings this-sighting)))))

(s/defn datetime-comparison :- s/Bool
  "Predicate for whether photo-a is prior to photo-b.
`f' is a function applied to both prior to the comparison."
  [f ta tb]
  (t/after? (get tb f) (get ta f)))

(s/defn independent-sightings-by-species
  [state sightings]
  (let [indep-reducer (partial independence-reducer state)]
    (->> sightings
         (filter :media-capture-timestamp)
         (sort (partial datetime-comparison :media-capture-timestamp))
         (reduce indep-reducer {}))))

(s/defn ->independent-sightings
  [state sightings]
  (->> (independent-sightings-by-species state sightings)
       vals
       flatten
       (sort-by :media-capture-timestamp)))

(s/defn extract-independent-sightings
  "Extract the sightings, accounting for the independence threshold, for an album."
  [state sightings]
  (let [total-spp (fn [[spp data]] {:species-id spp
                                    :count (reduce + 0 (map :sighting-quantity data))})]
    (map total-spp (independent-sightings-by-species state sightings))))
