(ns camelot.analysis.maxent
  (:require [clojure.string :as str]
            [camelot.model.album :refer [Album]]
            [schema.core :as s]))

(defn- species-location-reducer
  [acc photo]
  (let [loc (:location photo)]
    (if (and (:gps-longitude loc) (:gps-latitude loc))
      (apply conj acc (map #(vector (:species %)
                                    (:gps-longitude loc)
                                    (:gps-latitude loc))
                           (:sightings photo)))
      acc)))

(s/defn species-location-csv :- s/Str
  "Produce a CSV of species locations"
  [state
   albums :- {s/Str Album}]
  (let [photos (mapcat #(vals (:photos (second %))) albums)]
    (str/join "\n"
              (map #(str/join "," %)
                   (reduce species-location-reducer [] photos)))))
