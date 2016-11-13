(ns camelot.test-util.album
  (:require
   [camelot.model.import :as mi]
   [clojure.java.io :as io]
   [clj-time.core :as t]
   [clj-time.core :as t]))

(def std-location
  {:gps-longitude 100.0
   :gps-latitude -15
   :gps-altitude "50"
   :sublocation "Stables"
   :city "Camelot"
   :state "Norfolk"
   :country "Albion"
   :country-code "AL"
   :map-datum "ABC-123"})

(defn std-photo
  [p loc sightings]
  {:datetime (or (:datetime p) (t/date-time 2015 4 5 6 37 59))
   :datetime-original (or (:datetime-original p) nil)
   :headline (or (:headline p) "AHeadline")
   :artist (or (:artist p) "MyCo")
   :copyright (or (:copyright p) "2016 MyCo")
   :description (or (:description p) "APhoto")
   :filename (or (:filename p) "MyFile.jpg")
   :filesize (or (:filesize p) 320103)
   :sightings sightings
   :camera nil
   :settings nil
   :location loc})

(defn as-photo
  [p]
  (let [loc (mi/location (merge std-location (:location p)))
        sightings (map #(mi/sighting %) (:sightings p))]
    (mi/photo (std-photo p loc sightings))))

(defn as-album
  [photos]
  {:problems []
   :metadata {:datetime-start (t/now)
              :datetime-end (t/now)
              :make nil
              :model nil}
   :photos (into {} (map (fn [[k v]] (vector (io/file k) (as-photo v))) photos))})

(defn as-albums
  [k album]
  {(io/file k) (as-album album)})

(defn gen-filename
  [n]
  (format "File%d" n))
