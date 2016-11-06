(ns camelot.report.module.builtin.columns.media-capture-time-breakdown-test
  (:require
   [camelot.report.module.builtin.columns.media-capture-time-breakdown :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [camelot.test-util.state :as state]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en} config)))

(deftest test-calculate-media-capture-date
  (testing "Media capture timestamp breakdown"
    (testing "Calculates breakdown for date"
      (let [data [{:trap-station-id 1
                   :taxonomy-id 1
                   :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                  {:trap-station-id 1
                   :taxonomy-id 1
                   :media-capture-timestamp (t/date-time 2015 1 10 23 30 0)}]]
        (is (= (->> data
                    (sut/calculate-media-capture-date (gen-state-helper {}))
                    (map :media-capture-date)) ["2015-01-10" "2015-01-10"]))))

    (testing "Calculates breakdown for time"
      (let [data [{:trap-station-id 1
                   :taxonomy-id 1
                   :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                  {:trap-station-id 1
                   :taxonomy-id 1
                   :media-capture-timestamp (t/date-time 2015 1 10 23 30 10)}]]
        (is (= (->> data
                    (sut/calculate-media-capture-time (gen-state-helper {}))
                    (map :media-capture-time)) ["05:00:00" "23:30:10"]))))))
