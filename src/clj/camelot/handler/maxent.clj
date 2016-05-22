(ns camelot.handler.maxent
  (:require [camelot.model.album :refer [Album]]
            [camelot.processing
             [album :as album]
             [photo :as photo]]
            [camelot.util.config :as conf]
            [camelot.util.application :as app]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [schema.core :as s]))

(def output-descriptor
  "Paths for the output format"
  [{:path [:sighting :species] :required true}
   {:path [:location :gps-longitude] :required true}
   {:path [:location :gps-latitude] :required true}])

(defn build-row-descriptor
  "Return a descriptor for the row.
(Like an output descriptor, but includes the row's value.)"
  [photo]
  (mapv #(assoc % :value (photo/extract-path-value photo (:path %)))
        output-descriptor))

(defn contains-required-data?
  "Predicate for whether all required fields are met."
  [row-descriptor]
  (if (:required row-descriptor)
    (not (nil? (:value row-descriptor)))
    true))

(defn- species-location-reducer
  "Reducing function for species location."
  [acc photo]
  (let [maybe-conj #(if (or (nil? %) (empty? %))
                       acc
                       (conj acc %))]
    (->> photo
         (photo/flatten-sightings)
         (map build-row-descriptor)
         (filter #(every? contains-required-data? %))
         (flatten)
         (mapv :value)
         (maybe-conj))))

(s/defn to-csv-string :- s/Str
  "Return data as a CSV string."
  [data]
  (with-open [io-str (java.io.StringWriter.)]
    (csv/write-csv io-str data)
    (.toString io-str)))

(s/defn species-location-csv :- s/Str
  "Produce a CSV of species locations"
  [state
   albums :- {java.io.File Album}]
  (->> albums
       (mapcat album/album-photos)
       (reduce species-location-reducer [])
       (to-csv-string)))

(defn export
  "Handler for an export request."
  []
  (let [conf (conf/config)
        albums (album/read-albums (app/gen-state conf) (:root-path conf))
        data (species-location-csv (app/gen-state conf) albums)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"maxent.csv\""))))

(def routes
  "MaxEnt routes."
  (context "/maxent" []
           (GET "/" [] (export))))
