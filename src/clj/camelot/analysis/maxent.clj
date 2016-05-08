(ns camelot.analysis.maxent
  (:require [clojure.string :as str]
            [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [ring.util.response :as r]
            [camelot.model.album :refer [Album]]
            [camelot.handler.albums :as albums]
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
   albums :- {java.io.File Album}]
  (let [photos (mapcat #(vals (:photos (second %))) albums)]
    (str/join "\n"
              (map #(str/join "," %)
                   (reduce species-location-reducer [] photos)))))

(defn export
  "Handler for an export request."
  []
  (let [conf (config)
        albums (albums/read-albums (gen-state conf) (:root-path conf))
        data (species-location-csv (gen-state conf) albums)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"maxent.csv\""))))

(def routes
  "MaxEnt routes."
  (context "/maxent" []
           (GET "/" [] (export))))
