(ns camelot.processing.photo-test
  (:require [camelot.fixtures.exif-test-metadata :refer :all]
            [camelot.processing.photo :refer :all]
            [camelot.util.application :as app]
            [clj-time.core :as t]
            [clojure.data :refer [diff]]
            [midje.sweet :refer :all]
            [schema.test :as st]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "Metadata parsing"
  (fact "Maginon metadata normalises okay"
    (let [config []
          output (parse (app/gen-state config) maginon-metadata)]
      (:filesize output) => 1175819
      (:make (:camera output)) => "Maginon"
      (:datetime output) => (t/date-time 2014 4 11 16 37 52)))

  (fact "Cuddeback metadata normalises okay"
    (let [config []
          output (parse (app/gen-state config) cuddeback-metadata)]
      (:filesize output) => 513653
      (:make (:camera output)) => "CUDDEBACK"
      (:datetime output) => (t/date-time 2014 4 11 19 47 46)))

  (fact "Metadata with nil required fields is not valid"
    (let [config {:language :en}
          res (parse (app/gen-state config) {})]
      (contains? res :invalid) => true
      (boolean (re-find #"Date/Time" (:invalid res))) => true)))

(facts "Timeshift"
  (fact "Small positive timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 1 26)
          dt-b (t/date-time 2015 3 14 13 3 26)]
      (get-time-difference dt-a dt-b) => 120))

  (fact "Small negative timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 5 26)
          dt-b (t/date-time 2015 3 14 13 2 10)]
      (get-time-difference dt-a dt-b) => -196))

  (fact "Large timeshift is correct"
    (let [dt-a (t/date-time 2015 3 14 13 5 26)
          dt-b (t/date-time 2001 3 14 13 5 26)]
      (get-time-difference dt-a dt-b) => -441763200)))
