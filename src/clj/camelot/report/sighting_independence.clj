(ns camelot.report.sighting-independence
  (:require [clj-time.core :as t]
            [schema.core :as s]
            [clojure.string :as str]))

(defn- add-sighting
  "Add a new (i.e., independent) sighting."
  [state previous-sightings this-sighting]
  (let [duration (:sighting-independence-minutes-threshold (:config state))]
    (conj previous-sightings (assoc this-sighting
                                    :sighting-independence-window-end
                                    (t/plus (:media-capture-timestamp this-sighting)
                                            (t/minutes duration))))))

(defn- unidentified?
  "Predicate for whether a value could be considered unidentified."
  [v]
  (or (nil? v)
      (= (str/lower-case v) "unidentified")))

(defn- most-specific
  "Return the most specific value of the two given."
  [v1 v2]
  (if (unidentified? v1)
    v2
    v1))

(defn- infer-attributes
  "Attempt to infer lifestage or sex of a dependent sighting."
  [sighting new-sighting]
  (assoc sighting
         :sighting-lifestage (most-specific (:sighting-lifestage sighting)
                                            (:sighting-lifestage new-sighting))
         :sighting-sex (most-specific (:sighting-sex sighting)
                                      (:sighting-sex new-sighting))))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [previous-sightings sighting this-sighting]
  (let [new-qty (max (or (get sighting :sighting-quantity) 0)
                     (or (:sighting-quantity this-sighting) 0))]
    (conj previous-sightings
          (infer-attributes (assoc sighting :sighting-quantity new-qty)
                            this-sighting))))

(defn- could=?
  "Predicate for whether two sighting features could be considered equal"
  [field s1 s2]
  (let [v1 (get s1 field)
        v2 (get s2 field)]
    (or (unidentified? v1)
        (unidentified? v2)
        (= v1 v2))))

(defn- dependent-sighting?
  "Predicate for whether the sighting would be dependent."
  [current existing]
  (let [curtime (:media-capture-timestamp current)]
    (and (could=? :sighting-sex current existing)
         (could=? :sighting-lifestage current existing)
         (or (= curtime (:media-capture-timestamp existing))
             (and (t/after? curtime (:media-capture-timestamp existing))
                  (t/before? curtime (:sighting-independence-window-end existing)))))))

(defn- first-dependent-sighting
  "Return the first dependent sighting, if any."
  [sighting existing]
  (first (filter (partial dependent-sighting? sighting) existing)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state acc this-sighting]
  (let [datetime (:media-capture-timestamp this-sighting)
        species (:taxonomy-id this-sighting)
        previous-sighting (first-dependent-sighting this-sighting (get acc species))
        known-sightings (get acc species)]
    (assoc acc species
           (if previous-sighting
             (update-sighting (remove #(= previous-sighting %) known-sightings)
                              previous-sighting this-sighting)
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
