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

(deftest test-at-or-before?
  (testing "at-or-before?"
    (testing "Should return true if first timestamp is before second timestamp"
      (is (sut/at-or-before? (t/date-time 2016 1 1 10 0 0)
                             (t/date-time 2016 1 1 10 30 0))))

    (testing "Should return true if first timestamp is the same as second timestamp"
      (is (sut/at-or-before? (t/date-time 2016 1 1 10 0 0)
                             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return false if first timestamp is after as second timestamp"
      (is (not (sut/at-or-before? (t/date-time 2016 1 1 10 30 0)
                                  (t/date-time 2016 1 1 10 0 0)))))))

(deftest test-at-or-after?
  (testing "at-or-after?"
    (testing "Should return false if first timestamp is before second timestamp"
      (is (not (sut/at-or-after? (t/date-time 2016 1 1 10 0 0)
                                  (t/date-time 2016 1 1 10 30 0)))))

    (testing "Should return true if first timestamp is the same as second timestamp"
      (is (sut/at-or-after? (t/date-time 2016 1 1 10 0 0)
                             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return true if first timestamp is after as second timestamp"
      (is (sut/at-or-after? (t/date-time 2016 1 1 10 30 0)
                             (t/date-time 2016 1 1 10 0 0))))))

(deftest test-latest
  (testing "latest"
    (testing "Should return the latest of 2 dates"
      (is (= (sut/latest (t/date-time 2016 1 1 10 30 0)
                         (t/date-time 2016 1 1 10 0 0))
             (t/date-time 2016 1 1 10 30 0))))

    (testing "Should return the latest of 3 dates"
      (is (= (sut/latest (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0)
                         (t/date-time 2016 1 1 10 0 0))
             (t/date-time 2016 1 1 10 30 0))))

    (testing "Should return the latest of 3 dates, regardless of order"
      (is (= (sut/latest (t/date-time 2016 1 1 10 0 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0))
             (t/date-time 2016 1 1 10 30 0))))

    (testing "Should return the latest of 5 dates"
      (is (= (sut/latest (t/date-time 2016 1 1 10 0 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0))
             (t/date-time 2016 1 1 10 30 0))))

    (testing "Should return the latest of 5 dates"
      (is (= (sut/latest (t/date-time 2016 1 1 10 40 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 10 0)
                         (t/date-time 2016 1 1 10 20 0)
                         (t/date-time 2016 1 1 10 35 0))
             (t/date-time 2016 1 1 10 40 0))))))

(deftest test-earliest
  (testing "earliest"
    (testing "Should return the earliest of 2 dates"
      (is (= (sut/earliest (t/date-time 2016 1 1 10 30 0)
                         (t/date-time 2016 1 1 10 0 0))
             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return the earliest of 3 dates"
      (is (= (sut/earliest (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0)
                         (t/date-time 2016 1 1 10 0 0))
             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return the earliest of 3 dates, regardless of order"
      (is (= (sut/earliest (t/date-time 2016 1 1 10 0 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0))
             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return the earliest of 5 dates"
      (is (= (sut/earliest (t/date-time 2016 1 1 10 0 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 30 0))
             (t/date-time 2016 1 1 10 0 0))))

    (testing "Should return the earliest of 5 dates"
      (is (= (sut/earliest (t/date-time 2016 1 1 10 40 0)
                         (t/date-time 2016 1 1 10 15 0)
                         (t/date-time 2016 1 1 10 10 0)
                         (t/date-time 2016 1 1 10 20 0)
                         (t/date-time 2016 1 1 10 35 0))
             (t/date-time 2016 1 1 10 10 0))))))
