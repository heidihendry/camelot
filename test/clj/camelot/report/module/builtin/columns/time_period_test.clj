(ns camelot.report.module.builtin.columns.time-period-test
  (:require [camelot.report.module.builtin.columns.time-period :as sut]
            [clojure.test :refer :all]
            [camelot.testutil.state :as state]
            [clj-time.core :as t]))

(deftest test-date-period
  (testing "date->period"
    (testing "Should assoc month for given field"
      (let [r (sut/date->period :from :time-period-start (state/gen-state)
                                [{:from (t/date-time 2016 7 5)}])]
        (is (= r [{:from (t/date-time 2016 7 5)
                   :time-period-start "2016-07-05"}]))))

    (testing "Should assoc nil for nil value"
      (let [r (sut/date->period :from :time-period-start (state/gen-state)
                                [{:from nil}])]
        (is (= r [{:from nil
                   :time-period-start nil}]))))))

(deftest test-before-reducer
  (testing "before-reducer"
    (testing "Given 2 dates, will return the earliest"
      (is (= (reduce sut/before-reducer [(t/date-time 2016 1 1)
                                         (t/date-time 2016 1 2)])
             (t/date-time 2016 1 1))))

    (testing "Given 2 dates, will return the earliest, regardless of ordering"
      (is (= (reduce sut/before-reducer [(t/date-time 2016 1 2)
                                         (t/date-time 2016 1 1)])
             (t/date-time 2016 1 1))))

    (testing "Ignores nil dates if interspersed in list"
      (is (= (reduce sut/before-reducer [(t/date-time 2016 1 2)
                                         nil
                                         (t/date-time 2016 1 3)])
             (t/date-time 2016 1 2))))

    (testing "Returns earliest of 3 dates"
      (is (= (reduce sut/before-reducer [(t/date-time 2016 1 2)
                                         (t/date-time 2016 1 5)
                                         (t/date-time 2016 1 3)])
             (t/date-time 2016 1 2))))

    (testing "Returns nil if all dates are nil"
      (is (= (reduce sut/before-reducer [nil nil])
             nil)))

    (testing "Returns nil if no dates provided"
      (is (= (reduce sut/before-reducer [])
             nil)))))

(deftest test-after-reducer
  (testing "after-reducer"
    (testing "Given 2 dates, will return the earliest"
      (is (= (reduce sut/after-reducer [(t/date-time 2016 1 1)
                                        (t/date-time 2016 1 2)])
             (t/date-time 2016 1 2))))

    (testing "Given 2 dates, will return the earliest, regardless of ordering"
      (is (= (reduce sut/after-reducer [(t/date-time 2016 1 2)
                                        (t/date-time 2016 1 1)])
             (t/date-time 2016 1 2))))

    (testing "Ignores nil dates if interspersed in list"
      (is (= (reduce sut/after-reducer [(t/date-time 2016 1 2)
                                        nil
                                        (t/date-time 2016 1 3)])
             (t/date-time 2016 1 3))))

    (testing "Returns earliest of 3 dates"
      (is (= (reduce sut/after-reducer [(t/date-time 2016 1 2)
                                        (t/date-time 2016 1 5)
                                        (t/date-time 2016 1 3)])
             (t/date-time 2016 1 5))))

    (testing "Returns nil if all dates are nil"
      (is (= (reduce sut/after-reducer [nil nil])
             nil)))

    (testing "Returns nil if no dates provided"
      (is (= (reduce sut/after-reducer [])
             nil)))))
