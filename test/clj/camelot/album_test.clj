(ns camelot.album-test
  (:require [midje.sweet :refer :all]
            [clojure.data :refer [diff]]
            [camelot.album :refer :all]
            [camelot.config :refer [gen-state]]
            [clj-time.core :as t]
            [schema.test :as st]
            [camelot.exif-test-metadata :refer :all]))

(namespace-state-changes (before :facts st/validate-schemas))

(def night (t/date-time 2015 1 1 0 0 0))
(def day (t/date-time 2015 1 1 12 0 0))

(def config
  {:infrared-iso-value-threshold 999
   :night-end-hour 5
   :night-start-hour 21
   :erroneous-infrared-threshold 0.5
   :sighting-independence-minutes-threshold 20})

(facts "infrared threshold"
  (fact "A photo which uses IR at night is okay"
    (let [album [{:datetime night :settings {:iso 1000}}]]
      (exceed-ir-threshold config album) => false))

  (fact "A photo which uses IR in the day is okay"
    (let [album [{:datetime day :settings {:iso 1000}}]]
      (exceed-ir-threshold config album) => false))

  (fact "A photo which does not use IR at night is not okay"
    (let [album [{:datetime night :settings {:iso 999}}]]
      (exceed-ir-threshold config album) => true))

  (fact "One valid and one invalid photo is not okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 999}}]]
      (exceed-ir-threshold config album) => true))

  (fact "Two valid and one invalid photos is okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 1000}}
                 {:datetime night :settings {:iso 1000}}]]
      (exceed-ir-threshold config album) => false)))

(facts "album creation"
  (fact "An album is created for a single file's metadata"
    (let [f (clojure.java.io/file "file")
          data {f maginon-metadata}
          result (album (gen-state config) data)]
      (:make (:camera (get (:photos result) f))) => "Maginon")))

(def sightings {:datetime (t/date-time 2015 1 1 0 0 0)
                :sightings [{:species "Smiley Wolf"
                             :quantity 3}]})
(def camera {:make "CamMaker" :model "MyCam"})
(def chrono-first {:datetime (t/date-time 2015 1 1 0 0 0) :camera camera})
(def chrono-second {:datetime (t/date-time 2015 1 1 12 0 0) :camera camera})
(def chrono-third {:datetime (t/date-time 2015 1 2 5 0 0) :camera camera})
(def chrono-last {:datetime (t/date-time 2015 1 2 12 0 0) :camera camera})

(facts "metadata extraction"
  (fact "Start date is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state config)
          result (extract-metadata state album)]
      (:datetime-start result) => (:datetime chrono-first)))

  (fact "End date is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state config)
          result (extract-metadata state album)]
      (:datetime-end result) => (:datetime chrono-last)))

  (fact "Make is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state config)
          result (extract-metadata state album)]
      (:make result) => "CamMaker"))

  (fact "Model is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state config)
          result (extract-metadata state album)]
      (:model result) => "MyCam"))

  (fact "Sightings are extracted"
    (let [album [sightings]
          state (gen-state config)
          result (extract-metadata state album)]
      (:sightings result) => {"Smiley Wolf" 3})))

(facts "species extraction"
  (fact "A single sighting is extracted"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 1}))

  (fact "Multiple species are extracted if present"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 06 05 00)
                  :sightings [{:species "Smiley Wolf" :quantity 2}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 1
                                                      "Smiley Wolf" 2}))

  (fact "Sightings are only considered independent if having sufficient temporal distance"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 06 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 07 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 3}))

  (fact "A sighting may later need to be updated with a higher quantity"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 06 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}]}
                 {:datetime (t/date-time 2015 01 01 07 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 07 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 4}))

  (fact "A single sighting may contain multiple species"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}
                              {:species "Smiley Wolf" :quantity 2}]}
                 {:datetime (t/date-time 2015 01 01 06 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}]}
                 {:datetime (t/date-time 2015 01 01 07 00 00)
                  :sightings [{:species "Smiley Wolf" :quantity 1}]}
                 {:datetime (t/date-time 2015 01 01 07 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}
                              {:species "Smiley Wolf" :quantity 5}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 4
                                                      "Smiley Wolf" 7})))
