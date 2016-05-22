(ns camelot.handler.maxent-test
  (:require [camelot.handler.maxent :as sut]
            [camelot.processing.album :as a]
            [camelot.test-util.album :as ua]
            [camelot.util.application :as app]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [midje.sweet :refer :all]
            [schema.test :as st]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "MaxEnt export"
  (fact "Valid data should output one result"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude 100.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (app/gen-state {}) albums) => "Yellow Spotted Cat,100.0,0.0\n"))

  (fact "Missing sighting information should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings []
                                                  :location {:gps-longitude 100.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (app/gen-state {}) albums) => ""))

  (fact "Missing GPS longitude should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude nil
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (app/gen-state {}) albums) => ""))

  (fact "Missing GPS latitude should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings [{:species "Yellow Spotted Cat"}]
                                                  :location {:gps-longitude 0.0
                                                             :gps-latitude nil}}})]
      (sut/species-location-csv (app/gen-state {}) albums) => ""))

  (fact "Empty sighting list should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {"File" {:sightings []
                                                  :location {:gps-longitude 0.0
                                                             :gps-latitude 0.0}}})]
      (sut/species-location-csv (app/gen-state {}) albums) => ""))

  (fact "Album without photos should not produce results"
    (let [albums (ua/as-albums "AnAlbum" {})]
      (sut/species-location-csv (app/gen-state {}) albums) => ""))

  (fact "Should cope with large numbers of photos"
    (let [entries 100
          album-data (ua/as-photo {:sightings [{:species "Yellow Spotted Cat"}]
                                   :location {:gps-longitude 100.0
                                              :gps-latitude 0.0}})
          data (reduce (fn [acc x]
                         (assoc acc (io/file (ua/gen-filename x))
                                album-data)) {} (range 0 entries))
          albums (ua/as-albums "MyPhoto" data)
          result (->> albums
                      (sut/species-location-csv (app/gen-state {}))
                      (str/split-lines))]
      (count result) => entries
      (first result) => "Yellow Spotted Cat,100.0,0.0"
      (last result) => "Yellow Spotted Cat,100.0,0.0"))

  (fact "Should cope with large numbers of albums"
    (let [entries 100
          album-data {"MyFile" {:sightings [{:species "Yellow Spotted Cat"}]
                                :location {:gps-longitude 100.0
                                           :gps-latitude 0.0}}}
          albums (reduce (fn [acc x]
                           (assoc acc (io/file (ua/gen-filename x))
                                  (ua/as-album album-data))) {} (range 0 entries))
          result (->> albums
                      (sut/species-location-csv (app/gen-state {}))
                      (str/split-lines))]
      (count result) => entries
      (first result) => "Yellow Spotted Cat,100.0,0.0"
      (last result) => "Yellow Spotted Cat,100.0,0.0")))
