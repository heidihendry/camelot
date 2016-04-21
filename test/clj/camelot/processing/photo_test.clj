(ns camelot.processing.photo-test
  (:require [midje.sweet :refer :all]
            [clojure.data :refer [diff]]
            [camelot.processing.photo :refer :all]
            [camelot.processing.settings :refer [gen-state]]
            [schema.test :as st]
            [clj-time.core :as t]
            [camelot.fixtures.exif-test-metadata :refer :all]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "metadata normalisation"
  (fact "Maginon metadata normalises okay"
    (let [config []
          output (normalise (gen-state config) maginon-metadata)]
      (:filesize output) => 1175819
      (:make (:camera output)) => "Maginon"
      (:datetime output) => (t/date-time 2014 4 11 16 37 52)))

  (fact "Cuddeback metadata normalises okay"
    (let [config []
          output (normalise (gen-state config) cuddeback-metadata)]
      (:filesize output) => 513653
      (:make (:camera output)) => "CUDDEBACK"
      (:datetime output) => (t/date-time 2014 4 11 19 47 46))))

(fact "Photo validation"
  (fact "Metadata missing a datetime is not valid"
    (let [metadata [{:filename "MyFile.jpg"}]
          config {:language :en}]
      (contains? (validate (gen-state config) metadata) :invalid) => true))

  (fact "Metadata with nil required fields is not valid"
    (let [metadata [{:filename nil
                     :datetime nil}]
          config {:language :en}
          res (validate (gen-state config) metadata)]
      (contains? res :invalid) => true
      (boolean (re-find #"Date/Time" (:invalid res))) => true))

  (fact "Metadata with the necessary field is valid"
    (let [metadata {:filename "myfile"
                    :datetime 0}]
      (contains? (validate (gen-state []) metadata) :invalid) => false)))

(facts "GPS Date/Time"
  (fact "Produces the correct date from the components"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date "2015:03:14"
          time "13:01:26.33 UTC"]
      (exif-gps-datetime date time) => expected))

  (fact "nil if date is nil"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date nil
          time "13:01:26.33 UTC"]
      (exif-gps-datetime date time) => nil))

  (fact "nil if time is nil"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date "2015:03:14"
          time nil]
      (exif-gps-datetime date time) => nil)))

(facts "Timeshift"
  (fact "Small positive timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 1 26)
          dt-b (t/date-time 2015 3 14 13 3 26)]
      (get-timeshift dt-a dt-b) => 120))

  (fact "Small negative timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 5 26)
          dt-b (t/date-time 2015 3 14 13 2 10)]
      (get-timeshift dt-a dt-b) => -196))

  (fact "Large timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 5 26)
          dt-b (t/date-time 2001 3 14 13 5 26)]
      (get-timeshift dt-a dt-b) => -441763200)))
