(ns camelot.report.maxent-test
  (:require [camelot.application :as app]
            [camelot.report.core :as sut]
            [clojure
             [edn :as edn]
             [string :as str]]
            [clojure.java.io :as io]
            [midje.sweet :refer :all]
            [schema.test :as st]))

(namespace-state-changes (before :facts st/validate-schemas))

(def heading
  "Media ID,Species Scientific Name,Trap Station Longitude,Trap Station Latitude\n")

(def csv-report
  (partial sut/csv-report :maxent))

(facts "MaxEnt export"
  (fact "Valid data should output one result"
    (let [sightings [{:media-id 1
                      :survey-id 1
                      :species-scientific-name "Yellow Spotted Cat"
                      :trap-station-longitude 100.0
                      :trap-station-latitude 0.0}]]
      (csv-report (app/gen-state {:language :en}) 1 sightings) =>
      (str heading "1,Yellow Spotted Cat,100.0,0.0\n")))

  (fact "Missing sighting information should not produce results"
    (let [sightings [{:media-id 1
                      :survey-id 1
                      :species-scientific-name nil
                      :trap-station-longitude 100.0
                      :trap-station-latitude 0.0}]]
      (csv-report (app/gen-state {:language :en}) 1 sightings) => heading))

  (fact "Missing GPS longitude should not produce results"
    (let [sightings [{:media-id 1
                      :survey-id 1
                      :species-scientific-name "Yellow Spotted Cat"
                      :trap-station-longitude nil
                      :trap-station-latitude 0.0}]]
      (csv-report (app/gen-state {:language :en}) 1 sightings) => heading))

  (fact "Missing GPS latitude should not produce results"
    (let [sightings [{:media-id 1
                      :survey-id 1
                      :species-scientific-name "Yellow Spotted Cat"
                      :trap-station-longitude 100.0
                      :trap-station-latitude nil}]]
      (csv-report (app/gen-state {:language :en}) 1 sightings) => heading))

  (fact "Should exclude results from other surveys"
    (let [sightings [{:media-id 1
                      :survey-id 1
                      :species-scientific-name "Yellow Spotted Cat"
                      :trap-station-longitude 100.0
                      :trap-station-latitude 0.0}
                     {:media-id 1
                      :survey-id 2
                      :species-scientific-name "Smiley Wolf"
                      :trap-station-longitude 50.0
                      :trap-station-latitude 30}]]
      (csv-report (app/gen-state {:language :en}) 1 sightings) =>
      (str heading "1,Yellow Spotted Cat,100.0,0.0\n")))

  (fact "Should cope with a large number of results"
    (let [counter (atom 0)
          sightings (repeatedly 1000
                                #(hash-map :media-id (swap! counter inc)
                                           :survey-id 1
                                           :species-scientific-name "Yellow Spotted Cat"
                                           :trap-station-longitude 100.0
                                           :trap-station-latitude 0.0))
          result (str/split (csv-report (app/gen-state {:language :en}) 1 sightings) #"\n")]
      (count result) => 1001
      (second result) => "1,Yellow Spotted Cat,100.0,0.0"
      (last result) => "1000,Yellow Spotted Cat,100.0,0.0"))

  (fact "Time should increase by less than 5-fold when doubling amount of records"
    (let [counter (atom 0)
          sightings500 (repeatedly 250
                                   #(hash-map :media-id (swap! counter inc)
                                              :survey-id 1
                                              :species-scientific-name "Yellow Spotted Cat"
                                              :trap-station-longitude 100.0
                                              :trap-station-latitude 0.0))
          runset #(csv-report (app/gen-state {:language :en}) 1 %)
          sightings1000 (repeatedly 500
                                    #(hash-map :media-id (swap! counter inc)
                                               :survey-id 1
                                               :species-scientific-name "Yellow Spotted Cat"
                                               :trap-station-longitude 100.0
                                               :trap-station-latitude 0.0))
          result500 (with-out-str (time (doall (repeatedly 4 #(runset sightings500)))))
          result1000 (with-out-str (time (doall (repeatedly 4 #(runset sightings1000)))))
          read-result #(edn/read-string (nth (str/split % #" ") 2))]
      (< (read-result result1000) (* 5 (read-result result500))) => true)))
