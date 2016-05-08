(ns camelot.analysis.maxent-test
  (:require [camelot.analysis.maxent :as sut]
            [camelot.processing.settings :refer [gen-state]]
            [clojure.string :as str]
            [schema.test :as st]
            [midje.sweet :refer :all]
            [camelot.test-util.album :as ua]
            [camelot.processing.album :as a]
            [clojure.java.io :as io]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "MaxEnt export"
  (fact "Valid data should output one result"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude 100.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (gen-state {}) albums) => "Yellow Spotted Cat,100.0,0.0"))

  (fact "Missing sighting information should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings []
                                                  :location {:gps-longitude 100.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (gen-state {}) albums) => ""))

  (fact "Missing GPS longitude should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude nil
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (gen-state {}) albums) => ""))

  (fact "Missing GPS latitude should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude 0.0
                                                             :gps-latitude nil}}})]
      (sut/species-location-csv (gen-state {}) albums) => ""))

  (fact "Empty sighting list should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings []
                                                  :location {:gps-longitude 0.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (gen-state {}) albums) => ""))

  (fact "Album without photos should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {})]
      (sut/species-location-csv (gen-state {}) albums) => ""))

  (fact "Should cope with large numbers of photos"
    (let [entries 10000
          album-data (ua/as-photo {:sightings [{:species "Yellow Spotted Cat"}]
                                   :location {:gps-longitude 100.0
                                              :gps-latitude 0.0}})
          data (reduce (fn [acc x]
                         (assoc acc (io/file (ua/gen-filename x))
                                album-data)) {} (range 0 entries))
          albums (ua/as-albums "MyPhoto" data)
          result (->> albums
                      (sut/species-location-csv (gen-state {}))
                      (str/split-lines))]
      (count result) => entries
      (first result) => "Yellow Spotted Cat,100.0,0.0"
      (last result) => "Yellow Spotted Cat,100.0,0.0"))

  (fact "Should cope with large numbers of albums"
    (let [entries 10000
          album-data {"MyFile" {:sightings [{:species "Yellow Spotted Cat"}]
                                :location {:gps-longitude 100.0
                                           :gps-latitude 0.0}}}
          albums (reduce (fn [acc x]
                           (assoc acc (io/file (ua/gen-filename x))
                                  (ua/as-album album-data))) {} (range 0 entries))
          result (->> albums
                      (sut/species-location-csv (gen-state {}))
                      (str/split-lines))]
      (count result) => entries
      (first result) => "Yellow Spotted Cat,100.0,0.0"
      (last result) => "Yellow Spotted Cat,100.0,0.0")))
