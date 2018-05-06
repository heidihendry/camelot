(ns camelot.import.template-test
  (:require [camelot.import.template :as sut]
            [clojure.test :refer :all]))

(deftest test-to-latlong
  (testing "GPS parsing"
    (testing "GPS data is interpretted sanely"
      (let [lon "104° 5' 44.56\""
            lon-ref "E"]
        (is (= (sut/to-longitude lon lon-ref) 104.095711))))

    (testing "Longitude west is negative"
      (let [lon "104° 5' 44.56\""
            lon-ref "W"]
        (is (= (sut/to-longitude lon lon-ref) -104.095711))))

    (testing "Latitude south is negative"
      (let [lat "30° 44' 11\""
            lat-ref "S"]
        (is (= (sut/to-latitude lat lat-ref) -30.736389))))))

(deftest test-default-mapping-assignment
  (testing "Default mapping assignment."
    (testing "should map known default values"
      (let [ps {"Camelot GPS Latitude" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"}))))

    (testing "should be additive."
      (let [ps {"Camelot GPS Latitude" true
                "Camelot GPS Longitude" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"
                :trap-station-longitude "Camelot GPS Longitude"}))))

    (testing "should ignore columns which to not have a default mapping."
      (let [ps {"Camelot GPS Latitude" true
                "Something" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"}))))))
