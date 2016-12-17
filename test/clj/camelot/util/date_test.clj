(ns camelot.util.date-test
  (:require [camelot.util.date :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]))

(deftest test-at-midnight
  (testing "at-midnight"
    (testing "should return time at midnight for the current date"
      (is (t/equal? (sut/at-midnight (t/date-time 2016 1 1 10 3 5))
                    (t/date-time 2016 1 1))))

    (testing "should be idempotent"
      (is (t/equal? (sut/at-midnight
                     (sut/at-midnight (t/date-time 2016 1 1 10 3 5)))
                    (t/date-time 2016 1 1))))))
