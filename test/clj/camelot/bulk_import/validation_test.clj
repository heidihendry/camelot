(ns camelot.bulk-import.validation-test
  (:require [camelot.bulk-import.validation :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]
            [clojure.string :as str]))

(def default-record
  {:trap-station-session-start-date (t/date-time 2016 1 1 0 0 0)
   :trap-station-session-end-date (t/date-time 2016 2 1 0 0 0)
   :media-capture-timestamp (t/date-time 2016 1 2 0 0 0)})

(defn ->record
  ([]
   default-record)
  ([params]
   (merge default-record params)))

(defn gen-state
  []
  (state/gen-state {:language :en}))

(defn check-within-session-date
  [data]
  (:result (sut/check-media-within-session-date (gen-state) data)))

(defn check-end-date-not-in-future
  [data]
  (:result (sut/check-session-end-date-not-in-future
            (gen-state) {:trap-station-session-end-date data})))

(defn check-start-before-end
  [start-date end-date]
  (:result (sut/check-session-start-before-end
            (gen-state)
            {:trap-station-session-start-date start-date
             :trap-station-session-end-date end-date})))

(defn check-camera-overlap
  [data]
  (sut/check-overlapping-camera-usage (gen-state) data))

(deftest test-check-media-within-session-date
  (testing "Media within session date"
    (testing "should be valid between start and end date."
      (let [data (->record)]
        (is (= (check-within-session-date data) :pass))))

    (testing "should be valid when on start date."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 1 1)})]
        (is (= (check-within-session-date data) :pass))))

    (testing "should be valid when on end date."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 2 1)})]
        (is (= (check-within-session-date data) :pass))))

    (testing "should be valid when at end of end date, should its time be at midnight."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 2 1 23 59 59)})]
        (is (= (check-within-session-date data) :pass))))

    (testing "should be invalid before start date."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 1 1)
                            :trap-station-session-start-date (t/date-time 2016 1 2)})]
        (is (= (check-within-session-date data) :fail))))

    (testing "should be invalid after end date."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 2 2)})]
        (is (= (check-within-session-date data) :fail))))

    (testing "should be invalid on day of end date, should it not end at midnight."
      (let [data (->record {:media-capture-timestamp (t/date-time 2016 2 1 23 59 59)
                            :trap-station-session-end-date (t/date-time 2016 2 1 1 10 0)})]
        (is (= (check-within-session-date data) :fail))))))

(deftest test-check-session-end-date-not-in-future
  (testing "check-session-end-date-not-in-future"
    (testing "should fail if end-date is in the future"
      (with-redefs [t/now #(t/date-time 2016 1 1 10 30 0)]
        (is (= (check-end-date-not-in-future (t/date-time 2016 1 1 10 30 30))
               :fail))))

    (testing "should pass if end-date is equal to now"
      (with-redefs [t/now #(t/date-time 2016 1 1 10 30 0)]
        (is (= (check-end-date-not-in-future (t/date-time 2016 1 1 10 30 0))
               :pass))))

    (testing "should pass if end-date is before now"
      (with-redefs [t/now #(t/date-time 2016 1 1 10 30 0)]
        (is (= (check-end-date-not-in-future (t/date-time 2016 1 1 9 30 0))
               :pass))))))

(deftest test-check-session-start-before-end
  (testing "check-session-start-before-end"
    (testing "should pass if start is before end"
      (is (= (check-start-before-end (t/date-time 2016 1 1 9 30 0)
                                             (t/date-time 2016 1 1 9 35 0))
             :pass)))

    (testing "should pass if start is the same as end"
      (is (= (check-start-before-end (t/date-time 2016 1 1 9 30 0)
                                     (t/date-time 2016 1 1 9 30 0))
             :pass)))

    (testing "should fail if start is after end"
      (is (= (check-start-before-end (t/date-time 2016 1 1 9 35 0)
                                     (t/date-time 2016 1 1 9 30 0))
             :fail)))))

(deftest test-list-record-problems
  (testing "list-record-problems"
    (testing "should return failing failed test for a single record"
      (is (= (sut/list-record-problems (gen-state)
                                       {:always-fail (fn [s r] (hash-map :result :fail))}
                                       [{}])
             [{:result :fail
               :test :always-fail
               :row 2}])))

    (testing "should return the correct row number for each entry"
      (is (= (sut/list-record-problems (gen-state)
                                       {:always-fail (fn [s r] (hash-map :result :fail))}
                                       [{} {}])
             [{:result :fail
               :test :always-fail
               :row 2}
              {:result :fail
               :test :always-fail
               :row 3}])))

    (testing "should add an entry for each failure"
      (is (= (sut/list-record-problems (gen-state)
                                       {:always-fail (fn [s r] (hash-map :result :fail))
                                        :always-fail2 (fn [s r] (hash-map :result :fail))}
                                       [{}])
             [{:result :fail
               :test :always-fail
               :row 2}
              {:result :fail
               :test :always-fail2
               :row 2}])))

    (testing "should omit all successful executions"
      (is (= (sut/list-record-problems (gen-state)
                                       {:always-passes (fn [s r] (hash-map :result :pass))}
                                       [{} {}])
             [])))

    (testing "should run default tests if no tests provided"
      (is (= (sut/list-record-problems
              (gen-state)
              [{:trap-station-session-start-date (t/date-time 2016 1 1 0 0 0)
                :trap-station-session-end-date (t/date-time 2016 2 1 0 0 0)
                :media-capture-timestamp (t/date-time 2016 2 2 0 0 0)}])
             [{:result :fail
               :test :session-dates
               :row 2}])))))

(deftest test-overlapping-camera-usage
  (testing "check-overlapping-camera-usage"
    (testing "should return empty list if given empty list"
      (is (= (check-camera-overlap []) [])))

    (testing "should return empty list should different cameras overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 1)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 1 15)
                   :trap-station-session-end-date (t/date-time 2016 2 15)
                   :camera-name "CAM2"}]]
        (is (= (check-camera-overlap data) []))))

    (testing "should return empty list should the camera dates not overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 1)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 2)
                   :trap-station-session-end-date (t/date-time 2016 2 28)
                   :camera-name "CAM1"}]]
        (is (= (check-camera-overlap data) []))))

    (testing "should return empty list even if the camera stops and starts again on the same day"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 1)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 1)
                   :trap-station-session-end-date (t/date-time 2016 2 28)
                   :camera-name "CAM1"}]]
        (is (= (check-camera-overlap data) []))))

    (testing "should indicate an overlap should the camera overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 1)
                   :trap-station-session-end-date (t/date-time 2016 2 28)
                   :camera-name "CAM1"}]]
        (is (= (check-camera-overlap data)
               [{:result :fail
                 :reason "CAM1 is used in multiple sessions between 2016-02-01 and 2016-02-05"}]))))

    (testing "should indicate if there are multiple overlaps for a camera"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 1)
                   :trap-station-session-end-date (t/date-time 2016 2 28)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 10)
                   :trap-station-session-end-date (t/date-time 2016 3 10)
                   :camera-name "CAM1"}]
            result (:reason (first (check-camera-overlap data)))]
        (are [substr] (str/includes? result substr)
          "2016-02-01 and 2016-02-05"
          "2016-02-10 and 2016-02-28")))

    (testing "should show the expected dates for a total overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 2)
                   :trap-station-session-end-date (t/date-time 2016 2 4)
                   :camera-name "CAM1"}]
            result (:reason (first (check-camera-overlap data)))]
        (are [substr] (str/includes? result substr)
          "2016-02-02 and 2016-02-04")))

    (testing "should show the expected dates for a total overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 2)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 3)
                   :trap-station-session-end-date (t/date-time 2016 2 4)
                   :camera-name "CAM1"}]
            result (:reason (first (check-camera-overlap data)))]
        (is (str/includes? result "2016-02-02 and 2016-02-05"))
        (is (not (str/includes? result "2016-02-03 and 2016-02-04")))))

    (testing "should return overlaps for multiple cameras expected dates for a total overlap"
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 2)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 3)
                   :trap-station-session-end-date (t/date-time 2016 2 10)
                   :camera-name "CAM2"}
                  {:trap-station-session-start-date (t/date-time 2016 2 6)
                   :trap-station-session-end-date (t/date-time 2016 2 20)
                   :camera-name "CAM2"}]
            result (check-camera-overlap data)]
        (are [pos substr] (is (str/includes? (:reason (pos result)) substr))
          first "CAM1"
          first "2016-02-02 and 2016-02-05"
          second "CAM2"
          second "2016-02-06 and 2016-02-10")))))

(deftest test-validate
  (testing "validation"
    (testing "should return validation errors in a dataset."
      (let [data [{:trap-station-session-start-date (t/date-time 2016 1 1)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :media-capture-timestamp (t/date-time 2016 1 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 2)
                   :trap-station-session-end-date (t/date-time 2016 2 5)
                   :media-capture-timestamp (t/date-time 2016 1 5)
                   :camera-name "CAM1"}
                  {:trap-station-session-start-date (t/date-time 2016 2 3)
                   :trap-station-session-end-date (t/date-time 2016 2 10)
                   :media-capture-timestamp (t/date-time 2016 2 10)
                   :camera-name "CAM2"}
                  {:trap-station-session-start-date (t/date-time 2016 2 6)
                   :media-capture-timestamp (t/date-time 2016 2 10)
                   :trap-station-session-end-date (t/date-time 2016 2 20)
                   :camera-name "CAM2"}]
            result (sut/validate (gen-state) data)]
        (is (= (sort (map #(:test %) result))
               [:camera-overlaps
                :camera-overlaps
                :session-dates]))))))
