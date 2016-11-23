(ns camelot.import.validation-test
  (:require
   [camelot.import.validation :refer :all]
   [camelot.test-util.state :as state]
   [clj-time.core :as t]
   [clojure.test :refer :all]
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(def config
  {:infrared-iso-value-threshold 999
   :night-end-hour 5
   :night-start-hour 21
   :erroneous-infrared-threshold 0.5
   :sighting-independence-minutes-threshold 20})

(defn gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(def night (t/date-time 2015 1 1 0 0 0))
(def day (t/date-time 2015 1 1 12 0 0))

(deftest test-check-ir-threshold
  (testing "infrared threshold"
    (testing "A photo which uses IR at night is okay"
      (let [album [{:datetime night :settings {:iso 1000}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :pass))))

    (testing "A photo which uses IR in the day is okay"
      (let [album [{:datetime day :settings {:iso 1000}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :pass))))

    (testing "A photo which does not have IR should pass"
      (let [album [{:datetime day :settings {}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :pass))))

    (testing "A photo which does not use IR at night is not okay"
      (let [album [{:datetime night :settings {:iso 999}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :warn))))

    (testing "One valid and one invalid photo is not okay"
      (let [album [{:datetime night :settings {:iso 999}}
                   {:datetime day :settings {:iso 999}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :warn))))

    (testing "Two valid and one invalid photos is okay"
      (let [album [{:datetime night :settings {:iso 999}}
                   {:datetime day :settings {:iso 1000}}
                   {:datetime night :settings {:iso 1000}}]]
        (is (= (:result (check-ir-threshold (gen-state-helper config) album)) :pass))))))

(deftest test-check-photo-stddev
  (testing "Check: datetime std. dev"
    (testing "Does not flag std. dev issues if date/times are perfectly consistent"
      (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                   {:datetime (t/date-time 2015 01 01 06 10 00)}
                   {:datetime (t/date-time 2015 01 01 06 20 00)}
                   {:datetime (t/date-time 2015 01 01 06 30 00)}
                   {:datetime (t/date-time 2015 01 01 06 40 00)}
                   {:datetime (t/date-time 2015 01 01 06 50 00)}]]
        (is (= (:result (check-photo-stddev (gen-state-helper config) album)) :pass))))

    (testing "Does not flag std. dev issues if date/times are within thresholds"
      (let [album [{:datetime (t/date-time 2015 01 01 06 00 00)}
                   {:datetime (t/date-time 2015 01 14 06 10 00)}
                   {:datetime (t/date-time 2015 01 17 06 20 00)}
                   {:datetime (t/date-time 2015 01 10 06 30 00)}
                   {:datetime (t/date-time 2015 01 28 06 40 00)}
                   {:datetime (t/date-time 2015 01 21 06 50 00)}]]
        (is (= (:result (check-photo-stddev (gen-state-helper config) album)) :pass))))

    (testing "Does flag std. dev issues if one or more date/times deviations are excessive"
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
        (is (= (:result (check-photo-stddev (gen-state-helper config) album)) :warn))
        (is (= (boolean (re-find #"file1.*before" (:reason (check-photo-stddev
                                                            (gen-state-helper config) album)))) true))))

    (testing "An album with one photo passes"
      (let [album [{:datetime (t/date-time 2015 4 10 6 50 00)}]]
        (is (= (:result (check-photo-stddev (gen-state-helper config) album)) :pass))))))

(deftest test-check-project-dates
  (testing "Check: project start/end"
    (testing "Passes if dates are within project start/end"
      (let [config {:project-start (t/date-time 2015 1 1)
                    :project-end (t/date-time 2015 6 1)}
            photo {:datetime (t/date-time 2015 1 1 0 0 0)}]
        (is (= (:result (check-project-dates (gen-state-helper config) photo)) :pass))))

    (testing "Fails if a date is prior to the project start"
      (let [config {:project-start (t/date-time 2015 1 1)
                    :project-end (t/date-time 2015 6 1)}
            photo {:filename "file1"
                   :datetime (t/date-time 2014 12 31 0 0 0)}]
        (is (= (:result (check-project-dates (gen-state-helper config) photo)) :fail))
        (is (= (boolean
                (re-find #"file1"
                         (:reason
                          (check-project-dates (gen-state-helper config) photo)))) true))))

    (testing "Fails if a date is after the project end"
      (let [config {:project-start (t/date-time 2015 1 1)
                    :project-end (t/date-time 2015 6 1)}
            photo {:datetime (t/date-time 2016 1 1 0 0 0)}]
        (is (= (:result (check-project-dates (gen-state-helper config) photo)) :fail))))))

(deftest test-check-camera-checks
  (testing "Check: camera check"
    (testing "Albums with 2 camera checks on different days should pass"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                   {:datetime (t/date-time 2015 1 7 0 0 0)
                    :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
        (is (= (:result (check-camera-checks (gen-state-helper config) album)) :pass))))

    (testing "Albums with 1 camera check should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :sightings [{:species "Smiley Wolf" :quantity 1}]}
                   {:datetime (t/date-time 2015 1 7 0 0 0)
                    :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
        (is (= (:result (check-camera-checks (gen-state-helper config) album)) :warn))))

    (testing "Albums with 2 camera checks on the same day should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :sightings [{:species "HUMAN-CAMERACHECK" :quantity 1}]}]]
        (is (= (:result (check-camera-checks (gen-state-helper config) album)) :warn))))))

(deftest test-check-headline-consistency
  (testing "Album headline consistency"
    (testing "All files in an album containing the same headline should pass"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}]]
        (is (= (:result (check-headline-consistency (gen-state-helper config) album)) :pass))))

    (testing "Any one file not containing the same headline as the rest should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "XY-PSQ-02"}]]
        (is (= (:result (check-headline-consistency (gen-state-helper config) album)) :fail))))

    (testing "A missing headline should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :headline "AB-ABC-01"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)}]]
        (is (= (:result (check-headline-consistency (gen-state-helper config) album)) :fail))))))

(deftest test-check-source-consistency
  (testing "Album source consistency"
    (testing "All files in an album containing the same source should pass"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}]]
        (is (= (:result (check-source-consistency (gen-state-helper config) album)) :pass))))

    (testing "Any one file not containing the same source as the rest should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase2"}]]
        (is (= (:result (check-source-consistency (gen-state-helper config) album)) :fail))))

    (testing "A missing source should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :source "phase1"}
                   {:datetime (t/date-time 2015 1 5 0 0 0)}]]
        (is (= (:result (check-source-consistency (gen-state-helper config) album)) :fail))))))

(deftest test-check-camera-consistency
  (testing "Album camera consistency"
    (testing "All files in an album containing the same camera should pass"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :pass))))

    (testing "Any one file not containing the same camera as the rest should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "2.0"}}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :warn))))

    (testing "A missing camera should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :warn))))

    (testing "The model being different should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "notmymake"
                             :model "mymodel"
                             :software "1.0"}}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :warn))))

    (testing "The model being different should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "adifferentmodel"
                             :software "1.0"}}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :warn))))

    (testing "The software version being different should fail"
      (let [config {}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "1.0"}}
                   {:datetime (t/date-time 2015 1 5 0 0 0)
                    :camera {:make "mymake"
                             :model "mymodel"
                             :software "2.0"}}]]
        (is (= (:result (check-camera-consistency (gen-state-helper config) album)) :warn))))))

(deftest test-check-required-fields
  (testing "Required fields are respected"
    (testing "Required fields present across all files should pass"
      (let [config {:required-fields [[:headline] [:artist] [:phase] [:copyright]
                                      [:location :gps-longitude] [:location :gps-longitude-ref]
                                      [:location :gps-latitude] [:location :gps-latitude-ref]
                                      [:datetime] [:filename]]}
            photo {:filename "file1"
                   :datetime true
                   :headline true
                   :artist true
                   :phase true
                   :copyright true
                   :location {:gps-longitude true
                              :gps-longitude-ref true
                              :gps-latitude true
                              :gps-latitude-ref true}}]
        (is (= (:result (check-required-fields (gen-state-helper config) photo)) :pass))))

    (testing "Required fields missing from any files should fail"
      (let [config {:required-fields [[:headline] [:artist] [:phase] [:copyright]
                                      [:location :gps-longitude] [:location :gps-longitude-ref]
                                      [:location :gps-latitude] [:location :gps-latitude-ref]
                                      [:datetime] [:filename]]}
            ;; copyright missing
            photo {:filename "file1"
                   :datetime true
                   :headline true
                   :artist true
                   :phase true
                   :location {:gps-longitude true
                              :gps-longitude-ref true
                              :gps-latitude true
                              :gps-latitude-ref true}}]
        (is (= (:result (check-required-fields (gen-state-helper config) photo)) :fail))
        (is (= (boolean (re-find #"file1.*: Copyright"
                                 (:reason (check-required-fields
                                           (gen-state-helper config) photo)))) true))))))

(deftest test-check-album-has-data
  (testing "An album must have files"
    (testing "An empty album fails"
      (let [config {}
            album []]
        (is (= (:result (check-album-has-data (gen-state-helper config) album)) :fail))))

    (testing "An album with any files passes"
      (let [config {:project-start (t/date-time 2015 1 1)
                    :project-end (t/date-time 2015 6 1)}
            album [{:datetime (t/date-time 2015 1 5 0 0 0)}]]
        (is (= (:result (check-album-has-data (gen-state-helper config) album)) :pass))))))

(deftest test-check-sighting-consistency
  (testing "Sighting consistency"
    (testing "A sighting which has a species and quantity passes"
      (let [config {}
            photo {:sightings [{:species "Yellow Spotted Cat"
                                :quantity 1}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :pass))))

    (testing "A HUMAN-CAMERACHECK without a quantity passes"
      (let [config {}
            photo {:sightings [{:species "HUMAN-CAMERACHECK"}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :pass))))

    (testing "An unknown species without a quantity passes"
      (let [config {}
            photo {:sightings [{:species "Unknown"}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :pass))))

    (testing "A sighting which has a species but no quantity fails"
      (let [config {}
            photo {:filename "file1"
                   :sightings [{:species "Yellow Spotted Cat"}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :fail))
        (is (= (boolean
                (re-find #"file1" (:reason (check-sighting-consistency
                                            (gen-state-helper config) photo)))) true))))

    (testing "A sighting which has a quantity but no species fails"
      (let [config {}
            photo {:sightings [{:species nil
                                :quantity 1}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :fail))))

    (testing "A sighting missing a species fails"
      (let [config {}
            photo {:sightings [{:species nil
                                :quantity 1}]}]
        (is (= (:result (check-sighting-consistency (gen-state-helper config) photo)) :fail))))))

(deftest test-check-species
  (testing "Species check"
    (testing "Sightings with known species should pass"
      (let [config {:surveyed-species ["Smiley Wolf"]}
            photo {:sightings [{:species "smiley wolf"
                                :quantity 1}]}]
        (is (= (:result (check-species (gen-state-helper config) photo)) :pass))))

    (testing "No sightings should pass"
      (let [config {:surveyed-species ["Smiley Wolf"]}
            photo {:sightings []}]
        (is (= (:result (check-species (gen-state-helper config) photo)) :pass))))

    (testing "Sightings with unknown species should fail"
      (let [config {:surveyed-species ["Smiley Wolf"]}
            photo {:filename "file1"
                   :sightings [{:species "yellow spotted cat"
                                :quantity 1}]}]
        (is (= (:result (check-species (gen-state-helper config) photo)) :fail))
        (is (= (boolean
                (re-find #"file1.*: 'yellow spotted cat'" (:reason (check-species
                                                                    (gen-state-helper config) photo)))) true))))

    (testing "Sightings with any unknown species should fail"
      (let [config {:surveyed-species ["Smiley Wolf"]}
            photo {:filename "file1"
                   :sightings [{:species "yellow spotted cat"
                                :quantity 1}
                               {:species "smiley wolf"
                                :quantity 1}]}]
        (is (= (:result (check-species (gen-state-helper config) photo)) :fail))
        (is (= (boolean
                (re-find #"file1.*: 'yellow spotted cat'" (:reason (check-species
                                                                    (gen-state-helper config) photo)))) true))))

    (testing "A sighting, even if it doesn't contain all known species, should pass"
      (let [config {:surveyed-species ["Smiley Wolf" "Yellow Spotted Can"]}
            photo {:filename "file1"
                   :sightings [{:species "smiley wolf"
                                :quantity 1}]}]
        (is (= (:result (check-species (gen-state-helper config) photo)) :pass))))))

(deftest test-check-future
  (testing "Future timestamps"
    (testing "A timestamp in the future fails"
      (let [photo {:datetime (t/date-time 2021 1 1 0 0 0)}]
        (with-redefs [t/now #(t/date-time 2020 1 1)]
          (is (= (:result (check-future (gen-state-helper {}) photo)) :fail)))))

    (testing "A timestamp in the past is okay"
      (let [photo {:datetime (t/date-time 2019 1 1 0 0 0)}]
        (with-redefs [t/now #(t/date-time 2020 1 1)]
          (is (= (:result (check-future (gen-state-helper {}) photo)) :pass)))))))

(deftest test-check-invalid-photos
  (testing "Invalid photos"
    (testing "An album with one invalid photo fails the test"
      (let [album [{:invalid "Date/Time, Filename"}]]
        (is (= (:result (check-invalid-photos (gen-state-helper {}) album)) :fail))
        (is (= (boolean (re-find #"Date/Time, Filename"
                                 (:reason (check-invalid-photos
                                           (gen-state-helper {}) album)))) true))))

    (testing "An album without any invalid photos passes the test"
      (let [album [{:datetime (t/date-time 2019 1 1 0 0 0)}]]
        (is (= (:result (check-invalid-photos (gen-state-helper {}) album)) :pass))))

    (testing "An album without any invalid photos passes the test"
      (let [album [{:datetime (t/date-time 2019 1 1 0 0 0)}
                   {:invalid "Date/Time"}]]
        (is (= (:result (check-invalid-photos (gen-state-helper {}) album)) :fail))
        (is (= (boolean (re-find #"Date/Time"
                                 (:reason (check-invalid-photos
                                           (gen-state-helper {}) album)))) true))))))
