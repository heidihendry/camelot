(ns camelot.import.metadata-utils-test
  (:require
   [camelot.import.metadata-utils :as sut]
   [clj-time.core :as t]
   [clojure.test :refer :all]))

(deftest test-exif-gps-datetime
  (testing "GPS Date/Time"
    (testing "Produces the correct date from the components"
      (let [expected (t/date-time 2015 3 14 13 1 26)
            date "2015:03:14"
            time "13:01:26.33 UTC"]
        (is (= (sut/exif-gps-datetime date time) expected))))

    (testing "nil if date is nil"
      (let [expected (t/date-time 2015 3 14 13 1 26)
            date nil
            time "13:01:26.33 UTC"]
        (is (= (sut/exif-gps-datetime date time) nil))))

    (testing "nil if time is nil"
      (let [expected (t/date-time 2015 3 14 13 1 26)
            date "2015:03:14"
            time nil]
        (is (= (sut/exif-gps-datetime date time) nil))))))

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
