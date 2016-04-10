(ns camelot.processing.validation-test
  (:require [midje.sweet :refer :all]
            [camelot.processing.validation :refer :all]
            [camelot.processing.settings :as settings]
            [schema.test :as st]
            [clj-time.core :as t]))

(namespace-state-changes (before :facts st/validate-schemas))

(def config
  {:infrared-iso-value-threshold 999
   :night-end-hour 5
   :night-start-hour 21
   :erroneous-infrared-threshold 0.5
   :sighting-independence-minutes-threshold 20})

(defn gen-state-helper
  [config]
  (settings/gen-state (assoc config :language :en)))

(def night (t/date-time 2015 1 1 0 0 0))
(def day (t/date-time 2015 1 1 12 0 0))

(facts "infrared threshold"
  (fact "A photo which uses IR at night is okay"
    (let [album [{:datetime night :settings {:iso 1000}}]]
      (:result (check-ir-threshold (gen-state-helper config) album)) => :pass))

  (fact "A photo which uses IR in the day is okay"
    (let [album [{:datetime day :settings {:iso 1000}}]]
      (:result (check-ir-threshold (gen-state-helper config) album)) => :pass))

  (fact "A photo which does not use IR at night is not okay"
    (let [album [{:datetime night :settings {:iso 999}}]]
      (:result (check-ir-threshold (gen-state-helper config) album)) => :fail))

  (fact "One valid and one invalid photo is not okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 999}}]]
      (:result (check-ir-threshold (gen-state-helper config) album)) => :fail))

  (fact "Two valid and one invalid photos is okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 1000}}
                 {:datetime night :settings {:iso 1000}}]]
      (:result (check-ir-threshold (gen-state-helper config) album)) => :pass)))

(facts "Check: datetime std. dev"
  (fact "Does not flag std. dev issues if date/times are perfectly consistent"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                 {:datetime (t/date-time 2015 01 01 06 10 00)}
                 {:datetime (t/date-time 2015 01 01 06 20 00)}
                 {:datetime (t/date-time 2015 01 01 06 30 00)}
                 {:datetime (t/date-time 2015 01 01 06 40 00)}
                 {:datetime (t/date-time 2015 01 01 06 50 00)}]]
      (:result (check-photo-stddev (gen-state-helper config) album)) => :pass))

  (fact "Does not flag std. dev issues if date/times are within thresholds"
    (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                 {:datetime (t/date-time 2015 01 14 06 10 00)}
                 {:datetime (t/date-time 2015 01 17 06 20 00)}
                 {:datetime (t/date-time 2015 01 10 06 30 00)}
                 {:datetime (t/date-time 2015 01 28 06 40 00)}
                 {:datetime (t/date-time 2015 01 21 06 50 00)}]]
      (:result (check-photo-stddev (gen-state-helper config) album)) => :pass))

  (fact "Does flag std. dev issues if one or more date/times deviations are excessive"
    (let [album [{:filename "file1"
                  :datetime (t/date-time 2015 1 29  6 00 00)}
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
      (:result (check-photo-stddev (gen-state-helper config) album)) => :fail
      (boolean (re-find #"file1.*before" (:reason (check-photo-stddev
                                           (gen-state-helper config) album)))) => true))

  (fact "An album with one photo passes"
    (let [album [{:datetime (t/date-time 2015 4 10 6 50 00)}]]
      (:result (check-photo-stddev (gen-state-helper config) album)) => :pass)))

(facts "Check: project start/end"
  (fact "Passes if dates are within project start/end"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 1 0 0 0)}
                 {:datetime (t/date-time 2015 6 1 0 0 0)}]]
      (:result (check-project-dates (gen-state-helper config) album)) => :pass))

  (fact "Fails if a date is prior to the project start"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:filename "file1"
                  :datetime (t/date-time 2014 12 31 0 0 0)}
                 {:filename "file2"
                  :datetime (t/date-time 2015 6 1 0 0 0)}]]
      (:result (check-project-dates (gen-state-helper config) album)) => :fail
      (boolean
       (re-find #"file1"
                (:reason
                 (check-project-dates (gen-state-helper config) album)))) => true))

  (fact "Fails if a date is after the project end"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)}
                 {:datetime (t/date-time 2016 1 1 0 0 0)}]]
      (:result (check-project-dates (gen-state-helper config) album)) => :fail)))

(facts "Check: camera check"
  (fact "Albums with 2 camera checks on different days should pass"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 7 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (:result (check-camera-checks (gen-state-helper config) album)) => :pass))

  (fact "Albums with 1 camera check should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "Smiley Wolf" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 7 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (:result (check-camera-checks (gen-state-helper config) album)) => :fail))

  (fact "Albums with 2 camera checks on the same day should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
      (:result (check-camera-checks (gen-state-helper config) album)) => :fail)))

(facts "Album headline consistency"
  (fact "All files in an album containing the same headline should pass"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}]]
      (:result (check-headline-consistency (gen-state-helper config) album)) => :pass))

  (fact "Any one file not containing the same headline as the rest should fail"
    (let [config {}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "AB-ABC-01"}
                 {:datetime (t/date-time 2015 1 5 0 0 0)
                  :headline "XY-PSQ-02"}]]
      (:result (check-headline-consistency (gen-state-helper config) album)) => :fail)))

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
      (:result (check-required-fields (gen-state-helper config) album)) => :pass))

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
      (:result (check-required-fields (gen-state-helper config) album)) => :fail)))

(facts "An album must have files"
  (fact "An empty album fails"
    (let [config {}
          album []]
      (:result (check-album-has-data (gen-state-helper config) album)) => :fail))

  (fact "An album with any files passes"
    (let [config {:project-start (t/date-time 2015 1 1)
                  :project-end (t/date-time 2015 6 1)}
          album [{:datetime (t/date-time 2015 1 5 0 0 0)}]]
      (:result (check-album-has-data (gen-state-helper config) album)) => :pass)))

(fact "Sighting consistency"
  (fact "A sighting which has a species and quantity passes"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity 1}]}]]
      (:result (check-sighting-consistency (gen-state-helper config) album)) => :pass))

  (fact "A sighting which has a species but no quantity fails"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity nil}]}]]
      (:result (check-sighting-consistency (gen-state-helper config) album)) => :fail))

  (fact "A sighting which has a quantity but no species fails"
    (let [config {}
          album [{:sightings [{:species nil
                               :quantity 1}]}]]
      (:result (check-sighting-consistency (gen-state-helper config) album)) => :fail))

  (fact "An album which contains a bad sighting fails"
    (let [config {}
          album [{:sightings [{:species "Yellow Spotted Cat"
                               :quantity 1}]}
                 {:sightings [{:species nil
                               :quantity 1}]}
                 {:sightings [{:species "Smiley Wolf"
                               :quantity 2}]}]]
      (:result (check-sighting-consistency (gen-state-helper config) album)) => :fail)))

(facts "Species check"
  (fact "Sightings with known species should pass"
    (let [config {:surveyed-species ["Smiley Wolf"]}
          album [{:sightings [{:species "smiley wolf"
                               :quantity 1}]}]]
      (:result (check-species (gen-state-helper config) album)) => :pass))

  (fact "Sightings with known species should pass"
    (let [config {:surveyed-species ["Smiley Wolf"]}
          album [{:sightings [{:species "yellow spotted cat"
                               :quantity 1}]}]]
      (:result (check-species (gen-state-helper config) album)) => :fail)))

(facts "Future timestamps"
  (fact "A timestamp in the future fails"
    (let [album [{:datetime (t/date-time 2021 1 1 0 0 0)}]]
      (with-redefs [t/now #(t/date-time 2020 1 1)]
        (:result (check-future (gen-state-helper {}) album)) => :fail)))

  (fact "A timestamp in the past is okay"
    (let [album [{:datetime (t/date-time 2019 1 1 0 0 0)}]]
      (with-redefs [t/now #(t/date-time 2020 1 1)]
        (:result (check-future (gen-state-helper {}) album)) => :pass))))
