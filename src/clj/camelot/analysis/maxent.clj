(ns camelot.analysis.maxent
  (:require [clojure.string :as str]))

(defn species-location-reducer
  [acc photo]
  (concat acc (map #(vector (:species %)
                            (:gps-longitude (:location photo))
                            (:gps-latitude (:location photo)))
                   (:sightings photo))))

(defn species-location-csv
  [state albums]
  (let [photos (mapcat #(vals (:photos (second %))) albums)]
    (str/join "\n"
              (map #(str/join "," %)
                   (reduce species-location-reducer [] photos)))))
