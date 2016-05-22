(ns camelot.processing.metadata-utils-test
  (:require [camelot.processing.metadata-utils :as sut]
            [clj-time.core :as t]
            [midje.sweet :refer :all]))

(facts "GPS Date/Time"
  (fact "Produces the correct date from the components"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date "2015:03:14"
          time "13:01:26.33 UTC"]
      (sut/exif-gps-datetime date time) => expected))

  (fact "nil if date is nil"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date nil
          time "13:01:26.33 UTC"]
      (sut/exif-gps-datetime date time) => nil))

  (fact "nil if time is nil"
    (let [expected (t/date-time 2015 3 14 13 1 26)
          date "2015:03:14"
          time nil]
      (sut/exif-gps-datetime date time) => nil)))

(facts "GPS parsing"
  (fact "GPS data is interpretted sanely"
    (let [lon "104° 5' 44.56\""
          lon-ref "E"]
      (sut/to-longitude lon lon-ref) => 104.095711))

  (fact "Longitude west is negative"
    (let [lon "104° 5' 44.56\""
          lon-ref "W"]
      (sut/to-longitude lon lon-ref) => -104.095711))

  (fact "Latitude south is negative"
    (let [lat "30° 44' 11\""
          lat-ref "S"]
      (sut/to-latitude lat lat-ref) => -30.736389)))
