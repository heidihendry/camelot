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
      (check-ir-threshold (gen-state config) album) => :pass))

  (fact "A photo which uses IR in the day is okay"
    (let [album [{:datetime day :settings {:iso 1000}}]]
      (check-ir-threshold (gen-state config) album) => :pass))

  (fact "A photo which does not use IR at night is not okay"
    (let [album [{:datetime night :settings {:iso 999}}]]
      (check-ir-threshold (gen-state config) album) => :fail))

  (fact "One valid and one invalid photo is not okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 999}}]]
      (check-ir-threshold (gen-state config) album) => :fail))

  (fact "Two valid and one invalid photos is okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 1000}}
                 {:datetime night :settings {:iso 1000}}]]
      (check-ir-threshold (gen-state config) album) => :pass)))

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
                                                      "Smiley Wolf" 7}))

  (fact "Results are correct regardless of ordering of input"
    (let [album [{:datetime (t/date-time 2015 01 01 06 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}]}
                 {:datetime (t/date-time 2015 01 01 06 00 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 1}
                              {:species "Smiley Wolf" :quantity 2}]}
                 {:datetime (t/date-time 2015 01 01 07 10 00)
                  :sightings [{:species "Yellow Spotted Housecat" :quantity 2}
                              {:species "Smiley Wolf" :quantity 5}]}
                 {:datetime (t/date-time 2015 01 01 07 00 00)
                  :sightings [{:species "Smiley Wolf" :quantity 1}]}]
          state (gen-state config)]
      (extract-independent-sightings state album) => {"Yellow Spotted Housecat" 4
                                                      "Smiley Wolf" 7})))

(facts "Check: datetime std. dev"
  (fact "Does not flag std. dev issues if date/times are perfectly consistent"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                 {:datetime (t/date-time 2015 01 01 06 10 00)}
                 {:datetime (t/date-time 2015 01 01 06 20 00)}
                 {:datetime (t/date-time 2015 01 01 06 30 00)}
                 {:datetime (t/date-time 2015 01 01 06 40 00)}
                 {:datetime (t/date-time 2015 01 01 06 50 00)}]]
      (check-photo-stddev (gen-state config) album) => :pass))

  (fact "Does not flag std. dev issues if date/times are within thresholds"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                 {:datetime (t/date-time 2015 01 14 06 10 00)}
                 {:datetime (t/date-time 2015 01 17 06 20 00)}
                 {:datetime (t/date-time 2015 01 10 06 30 00)}
                 {:datetime (t/date-time 2015 01 28 06 40 00)}
                 {:datetime (t/date-time 2015 01 21 06 50 00)}]]
      (check-photo-stddev (gen-state config) album) => :pass))

  (fact "Does flag std. dev issues if one or more date/times deviations are excessive"
    (let [album [{:datetime (t/date-time 2015 1 1  6 00 00)}
                 {:datetime (t/date-time 2015 3 24 6 10 00)}
                 {:datetime (t/date-time 2015 3 25 6 20 00)}
                 {:datetime (t/date-time 2015 3 26 6 30 00)}
                 {:datetime (t/date-time 2015 3 27 6 40 00)}
                 {:datetime (t/date-time 2015 3 28 6 40 00)}
                 {:datetime (t/date-time 2015 3 30 6 40 00)}
                 {:datetime (t/date-time 2015 4 2  6 40 00)}
                 {:datetime (t/date-time 2015 4 4  6 40 00)}
                 {:datetime (t/date-time 2015 4 6  6 00)}
                 {:datetime (t/date-time 2015 4 7  6 40 00)}
                 {:datetime (t/date-time 2015 4 10 6 50 00)}]]
      (check-photo-stddev (gen-state config) album) => :fail)))

(facts "Check: project start/end"
  (fact "Passes if dates are within project start/end"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 1 0 0 0)}
                 {:datetime (t/date-time 2015 6 1 0 0 0)}]]
      (check-project-dates (gen-state config) album) => :pass))

  (fact "Fails if a date is prior to the project start"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2014 12 31 0 0 0)}
                 {:datetime (t/date-time 2015 6 1 0 0 0)}]]
      (check-project-dates (gen-state config) album) => :fail))

  (fact "Fails if a date is after the project end"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)}
                 {:datetime (t/date-time 2016 1 1 0 0 0)}]]
      (check-project-dates (gen-state config) album) => :fail)))

(facts "Check: camera check"
  (fact "Albums with 2 camera checks on different days should pass"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 7 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (check-camera-checks (gen-state config) album) => :pass))

  (fact "Albums with 1 camera check should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "Smiley Wolf" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 7 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (check-camera-checks (gen-state config) album) => :fail))

  (fact "Albums with 2 camera checks on the same day should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (check-camera-checks (gen-state config) album) => :fail)))

(facts "Album headline consistency"
  (fact "All files in an album containing the same headline should pass"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}]]
      (check-headline-consistency (gen-state config) album) => :pass))

  (fact "Any one file not containing the same headline as the rest should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "XY-PSQ-02"}]]
      (check-headline-consistency (gen-state config) album) => :fail)))

(facts "Required fields are respected"
  (fact "Required fields present across all files should pass"
    (let [config {:required-fields [[:headline] [:artist] [:phase] [:copyright]
                                    [:location :gps-longitude] [:location :gps-longitude-ref]
                                    [:location :gps-latitude] [:location :gps-latitude-ref]
                                    [:datetime] [:filename]]}
          album [{:datetime true
                  :headline true
                  :artist true
                  :phase true
                  :copyright true
                  :location {:gps-longitude true
                             :gps-longitude-ref true
                             :gps-latitude true
                             :gps-latitude-ref true}
                  :filename true}]]
      (check-required-fields (gen-state config) album) => :pass))

  (fact "Required fields missing from any files should fail"
    (let [config {:required-fields [[:headline] [:artist] [:phase] [:copyright]
                                    [:location :gps-longitude] [:location :gps-longitude-ref]
                                    [:location :gps-latitude] [:location :gps-latitude-ref]
                                    [:datetime] [:filename]]}
          ;; copyright missing
          album [{:datetime true
                  :headline true
                  :artist true
                  :phase true
                  :location {:gps-longitude true
                             :gps-longitude-ref true
                             :gps-latitude true
                             :gps-latitude-ref true}
                  :filename true}]]
      (check-required-fields (gen-state config) album) => :fail)))

(facts "An album must have files"
  (fact "An empty album fails"
    (let [config {}
          album []]
      (check-album-has-data (gen-state config) album) => :fail))

  (fact "An album with any files passes"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)}]]
      (check-album-has-data (gen-state config) album) => :pass)))

(fact "Sighting consistency"
  (fact "A sighting which has a species and quantity passes"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity 1}]}]]
      (check-sighting-consistency (gen-state config) album) => :pass))

  (fact "A sighting which has a species but no quantity fails"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity nil}]}]]
      (check-sighting-consistency (gen-state config) album) => :fail))

  (fact "A sighting which has a quantity but no species fails"
    (let [config {}
          album [{:sightings [{:species nil
                               :quantity 1}]}]]
      (check-sighting-consistency (gen-state config) album) => :fail))

  (fact "An album which contains a bad sighting fails"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity 1}]}
                 {:sightings [{:species nil
                               :quantity 1}]}
                 {:sightings [{:species "Smiley Wolf"
                               :quantity 2}]}]]
      (check-sighting-consistency (gen-state config) album) => :fail)))

(facts "Species check"
  (fact "Sightings with known species should pass"
    (let [config {:surveyed-species ["Smiley Wolf"]}
          album [{:sightings [{:species "smiley wolf"
                               :quantity 1}]}]]
      (check-species (gen-state config) album) => :pass))

  (fact "Sightings with known species should pass"
    (let [config {:surveyed-species ["Smiley Wolf"]}
          album [{:sightings [{:species "yellow spotted cat"
                               :quantity 1}]}]]
      (check-species (gen-state config) album) => :fail)))
