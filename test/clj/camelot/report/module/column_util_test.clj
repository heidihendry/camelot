(ns camelot.report.module.column-util-test
  (:require [camelot.report.module.column-util :as sut]
            [clojure.test :refer :all]
            [camelot.testutil.state :as state]
            [clj-time.core :as t]))

(defn calculate-nights-elapsed
  [state data]
  (sut/calculate-nights-elapsed state data))

(defn calculate-total-nights
  [state data]
  (sut/calculate-total-nights state data))

(deftest test-calculate-nights-elapsed
  (testing "calculate-nights-elapsed"
    (testing "should assoc number of nights elapsed for a session"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               (map #(assoc % :nights-elapsed 2) data)))))

    (testing "should not double-count nights if multiple sessions are active"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 4)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               [(assoc (first data) :nights-elapsed 2)
                (assoc (second data) :nights-elapsed 3)]))))

    (testing "should assoc zero if camera removed same day it was placed"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 1)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               (map #(assoc % :nights-elapsed 0) data)))))

    (testing "should assoc zero if all media is unrecoverable"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-media-unrecoverable true
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               (map #(assoc % :nights-elapsed 0) data)))))

    (testing "should give session start-end, even if multiple cameras active"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 1
                   :trap-station-session-camera-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               (map #(assoc % :nights-elapsed 2) data)))))

    (testing "should count session if media is recovered for a camera"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-id 1
                   :trap-station-session-camera-media-unrecoverable true
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 1
                   :trap-station-session-camera-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-nights-elapsed (state/gen-state) data)
               (map #(assoc % :nights-elapsed 2) data))))))

  (testing "calculate-total-nights"
    (testing "should assoc number of total nights for one session"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 2) data)))))

    (testing "should assoc nights as the sum of all active sessions"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 4)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 5) data)))))

    (testing "should assoc zero if camera removed same day it was placed"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 1)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 0) data)))))

    (testing "should assoc zero if all media is unrecoverable"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-media-unrecoverable true
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 0) data)))))

    (testing "should give session start-end, even if multiple cameras active"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-id 1
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 1
                   :trap-station-session-camera-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 2) data)))))

    (testing "should count session if media is recovered for a camera"
      (let [data [{:trap-station-session-id 1
                   :trap-station-session-camera-id 1
                   :trap-station-session-camera-media-unrecoverable true
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}
                  {:trap-station-session-id 1
                   :trap-station-session-camera-id 2
                   :trap-station-session-start-date (t/date-time 2017 1 1)
                   :trap-station-session-end-date (t/date-time 2017 1 3)}]]
        (is (= (calculate-total-nights (state/gen-state) data)
               (map #(assoc % :total-nights 2) data)))))))
