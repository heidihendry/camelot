(ns camelot.photo-test
  (:require [midje.sweet :refer :all]
            [clojure.data :refer [diff]]
            [camelot.photo :refer :all]
            [schema.test :as st]
            [clj-time.core :as t]
            [camelot.exif-test-metadata :refer :all]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "metadata normalisation"
  (fact "Maginon metadata normalises okay"
    (let [output (normalise maginon-metadata)]
      (:filesize output) => 1175819
      (:make (:camera output)) => "Maginon"
      (:datetime output) => (t/date-time 2014 4 11 16 37 52)))

  (fact "Cuddeback metadata normalises okay"
    (let [output (normalise cuddeback-metadata)]
      (:filesize output) => 513653
      (:make (:camera output)) => "CUDDEBACK"
      (:datetime output) => (t/date-time 2014 4 11 19 47 46))))
