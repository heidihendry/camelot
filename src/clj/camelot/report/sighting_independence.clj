(ns camelot.report.sighting-independence
  (:require [clj-time.core :as t]
            [schema.core :as s]))

(defn- add-sighting
  "Add a new (i.e., independent) sighting."
  [state previous-sightings datetime quantity]
  (let [duration (:sighting-independence-minutes-threshold (:config state))]
    (conj previous-sightings {:start datetime
                              :end (t/plus datetime (t/minutes duration))
                              :quantity quantity})))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [previous-sightings sighting quantity]
  (let [new-qty (max (or (get sighting :quantity) 0) quantity)]
    (conj previous-sightings
          (assoc sighting :quantity new-qty))))

(defn- dependent-sighting?
  "Predicate for whether the sighting would be dependent for a timespan."
  [sighting timespan]
  (or (= sighting (:start timespan))
      (and (t/after? sighting (:start timespan))
           (t/before? sighting (:end timespan)))))

(defn- dependent-sighting
  "Return the first dependent sighting, if any."
  [sighting datespans]
  (first (filter (partial dependent-sighting? sighting) datespans)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state acc this-sighting]
  (let [datetime (:media-capture-timestamp this-sighting)
        species (:species-scientific-name this-sighting)
        previous-sighting (dependent-sighting datetime (get acc species))
        qty (:sighting-quantity this-sighting)
        known-sightings (get acc species)]
    (assoc acc species
           (if previous-sighting
             (update-sighting (remove #(= previous-sighting %) known-sightings)
                              previous-sighting qty)
             (add-sighting state known-sightings datetime qty)))))

(defn- add-times-to-sightings
  "Assoc date/time information into the sighting."
  [p]
  (map #(assoc % :datetime (:datetime p)) (:sightings p)))

(s/defn datetime-comparison :- s/Bool
  "Predicate for whether photo-a is prior to photo-b.
`f' is a function applied to both prior to the comparison."
  [f ta tb]
  (t/after? (get tb f) (get ta f)))

(s/defn extract-independent-sightings
  "Extract the sightings, accounting for the independence threshold, for an album."
  [state sightings]
  (let [indep-reducer (partial independence-reducer state)
        total-spp (fn [[spp data]] {:species spp
                                    :count (reduce + 0 (map :quantity data))})]
    (->> sightings
         (sort (partial datetime-comparison :media-capture-timestamp))
         (reduce indep-reducer {})
         (map total-spp))))
