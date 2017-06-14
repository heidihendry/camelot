(ns camelot.util.sunrise-sunset-test
  (:require
   [camelot.util.sunrise-sunset :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t])
  (:import
   (java.util TimeZone)))

(def new-york (TimeZone/getTimeZone "America/New_York"))

(deftest test-sunrise-time
  (testing "Sunrise time"
    (testing "Should correctly calculate the sunrise time in UTC"
      (is (= (sut/sunrise-time new-york "40.7" "-74.0" (t/date-time 2016 6 1))
             (t/date-time 2016 5 31 5 27 0))))

    (testing "Should correctly calculate the sunset time in UTC"
      (is (= (sut/sunset-time new-york "40.7" "-74.0" (t/date-time 2016 6 1))
             (t/date-time 2016 5 31 20 21 0))))

    (testing "Should return nil when there is no sunrise"
      (is (= (sut/sunset-time new-york "85" "0" (t/date-time 2016 9 11))
             nil)))

    (testing "Should return nil when there is no sunset"
      (is (= (sut/sunset-time new-york "-85" "0" (t/date-time 2016 10 4))
             nil)))))
