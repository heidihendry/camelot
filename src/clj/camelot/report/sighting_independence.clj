(ns camelot.report.sighting-independence
  "Sighting independence transformations."
  (:require
   [clj-time.core :as t]
   [schema.core :as sch]
   [camelot.util.sighting-fields :as util.sf]))

(defn sighting-fields
  [state survey-id]
  (filter :sighting-field-affects-independence
          (get-in state [:survey-settings survey-id :sighting-fields])))

(defn independence-threshold
  [state survey-id]
  (get-in state [:survey-settings survey-id :survey-sighting-independence-threshold]))

(defn- add-sighting
  "Add a new (i.e., independent) sighting."
  [state previous-sightings this-sighting]
  (let [duration (or (independence-threshold state (:survey-id this-sighting))
                     20)]
    (conj previous-sightings (assoc this-sighting
                                    :sighting-independence-window-end
                                    (t/plus (:media-capture-timestamp this-sighting)
                                            (t/minutes duration))))))

(defn- unidentified?
  [v]
  (or (nil? v) (= v "")))

(defn- most-specific
  "Return the most specific value of the two given."
  [v1 v2]
  (if (unidentified? v1)
    v2
    v1))

(defn- infer-attributes
  "Infer dependent sighting using independent sighting field values."
  [state sighting new-sighting]
  (let [sighting-fields (sighting-fields state (:survey-id new-sighting))]
    (letfn [(reducer [acc x]
              (assoc acc x (most-specific (get sighting x)
                                          (get new-sighting x))))]
      (->> sighting-fields
           (map util.sf/user-key)
           (reduce reducer sighting)))))

(defn- sighting-quantity ^long
  [sighting]
  (or (get sighting :sighting-quantity) 0))

(defn- update-sighting
  "Update the set of previous (i.e., dependent) sightings"
  [state previous-sightings sighting this-sighting]
  (let [new-qty (max (sighting-quantity sighting)
                     (sighting-quantity this-sighting))]
    (conj previous-sightings
          (infer-attributes state
                            (assoc sighting :sighting-quantity new-qty)
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
  [state current existing]
  (let [curtime (:media-capture-timestamp current)]
    (and (every? #(could=? % current existing)
                 (map util.sf/user-key (sighting-fields state
                                                        (:survey-id current))))
         (or (= curtime (:media-capture-timestamp existing))
             (and (t/after? curtime (:media-capture-timestamp existing))
                  (t/before? curtime (:sighting-independence-window-end existing)))))))

(defn- first-dependent-sighting
  "Return the first dependent sighting, if any."
  [state sighting existing]
  (first (filter (partial dependent-sighting? state sighting) existing)))

(defn- independence-reducer
  "Reducing function, adding or updating the sightings based on their dependence."
  [state acc this-sighting]
  (let [species (:taxonomy-id this-sighting)
        previous-sighting (first-dependent-sighting state this-sighting (get acc species))
        known-sightings (get acc species)]
    (if (nil? species)
      acc
      (assoc acc species
             (if previous-sighting
               (update-sighting
                state
                (remove #(= previous-sighting %) known-sightings)
                previous-sighting this-sighting)
               (add-sighting state known-sightings this-sighting))))))

(sch/defn datetime-comparison :- sch/Bool
  "Predicate for whether photo-a is prior to photo-b.
`f' is a function applied to both prior to the comparison."
  [f ta tb]
  (t/after? (get tb f) (get ta f)))

(sch/defn independent-sightings-by-species
  [state sightings]
  (let [indep-reducer (partial independence-reducer state)]
    (->> sightings
         (filter :media-capture-timestamp)
         (sort (partial datetime-comparison :media-capture-timestamp))
         (reduce indep-reducer {}))))

(sch/defn sighting-group-independence
  "Check independence of a group of sightings where they may be considered dependent based on time and sighting information."
  [state sightings]
  (->> (independent-sightings-by-species state sightings)
       vals
       flatten
       (sort-by :media-capture-timestamp)))

(sch/defn ->independent-sightings
  "Process all records, subdividing by trap station session ID before checking
  independence."
  [state sightings]
  (->> sightings
       (group-by :trap-station-session-id)
       vals
       (map (partial sighting-group-independence state))
       (flatten)))

(sch/defn extract-independent-sightings
  "Extract the sightings, accounting for the independence threshold, for an album."
  [state sightings]
  (let [total-spp (fn [[spp data]] {:species-id spp
                                    :count (reduce + 0 (map #(or (:sighting-quantity %) 0)
                                                            data))})]
    (->> sightings
         (independent-sightings-by-species state)
         (map total-spp)
         (remove #(zero? (:count %))))))
