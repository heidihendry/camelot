(ns camelot.report.module.builtin.columns.presence-absence-test
  (:require [camelot.report.module.builtin.columns.presence-absence :as sut]
            [clojure.test :refer :all]
            [camelot.testutil.state :as state]
            [clj-time.core :as t]))

(defn calculate
  [data]
  (with-redefs [camelot.model.survey/survey-settings (constantly {})]
    (sut/calculate-presence-absence
     (state/gen-state {:sighting-independence-minutes-threshold 20})
     data)))

(defn aggregate
  [data]
  (sut/aggregate-presence-absence (state/gen-state) :ignored data))

(deftest test-calculate-presence-absence
  (testing "calculate-presence-absence"
    (testing "Should assoc nil if no sightings"
      (let [data [{}]]
        (is (= (calculate data)
               [{:independent-observations 0
                 :presence-absence ""}]))))

    (testing "Should assoc 'X' if independent sightings found"
      (let [data [{:media-capture-timestamp (t/date-time 2016 1 1 8)
                   :trap-station-session-id 1
                   :sighting-quantity 2
                   :taxonomy-id 1}]]
        (is (= (calculate data)
               [{:independent-observations 2
                 :media-capture-timestamp (t/date-time 2016 1 1 8)
                 :trap-station-session-id 1
                 :sighting-quantity 2
                 :taxonomy-id 1
                 :presence-absence "X"}]))))

    (testing "Should assoc '' if zero sighting quantity"
      (let [data [{:media-capture-timestamp (t/date-time 2016 1 1 8)
                   :trap-station-session-id 1
                   :sighting-quantity 0
                   :taxonomy-id 1}]]
        (is (= (calculate data)
               [{:independent-observations 0
                 :media-capture-timestamp (t/date-time 2016 1 1 8)
                 :trap-station-session-id 1
                 :sighting-quantity 0
                 :taxonomy-id 1
                 :presence-absence ""}])))))

  (testing "aggregate-presence-absence"
    (testing "Should return 'X' if trap station session has any sightings."
      (let [data [{:media-capture-timestamp (t/date-time 2016 1 1 8)
                   :trap-station-session-id 1
                   :independent-observations 1}]]
        (is (= (aggregate data) "X"))))

    (testing "Should return '' if trap station session has no sightings."
      (let [data [{:media-capture-timestamp (t/date-time 2016 1 1 8)
                   :trap-station-session-id 1
                   :independent-observations 0}]]
        (is (= (aggregate data) ""))))))
