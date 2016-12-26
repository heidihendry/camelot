(ns camelot.report.module.builtin.columns.percent-nocturnal-test
  (:require [camelot.report.module.builtin.columns.percent-nocturnal :as sut]
            [clojure.test :refer :all]
            [camelot.test-util.state :as state]
            [clj-time.core :as t]))

(defn calculate
  [data]
  (sut/calculate-is-night (state/gen-state {:timezone "America/New_York"}) data))

(defn aggregate
  [data]
  (sut/aggregate-is-night (state/gen-state {:sighting-independence-minutes-threshold 20})
                          :percent-nocturnal data))

(deftest test-percent-nocturnal
  (testing "calculate-is-night"
    (testing "should assoc to nil if required data unavailable"
      (let [data [{}]]
        (is (= (calculate data)
               [{:percent-nocturnal nil}]))))

    (testing "should assoc to nil if longitude unavailable"
      (let [data [{:media-capture-timestamp (t/date-time 2016 6 1)
                   :trap-station-latitude 40.713}]]
        (is (= (calculate data)
               [{:media-capture-timestamp (t/date-time 2016 6 1)
                 :trap-station-latitude 40.713
                 :percent-nocturnal nil}]))))

    (testing "should assoc to nil if latitude unavailable"
      (let [data [{:media-capture-timestamp (t/date-time 2016 6 1)
                   :trap-station-longitude -74.005}]]
        (is (= (calculate data)
               [{:media-capture-timestamp (t/date-time 2016 6 1)
                 :trap-station-longitude -74.005
                 :percent-nocturnal nil}]))))

    (testing "should assoc to nil if media timestamp unavailable"
      (let [data [{:trap-station-latitude 40.713
                   :trap-station-longitude -74.005}]]
        (is (= (calculate data)
               [{:trap-station-latitude 40.713
                 :trap-station-longitude -74.005
                 :percent-nocturnal nil}]))))

    (testing "should assoc to 'X' if captured at night"
      (let [data [{:trap-station-longitude -74.005
                   :trap-station-latitude 40.713
                   :media-capture-timestamp (t/date-time 2016 6 1)}]]
        (is (= (calculate data)
               [{:trap-station-longitude -74.005
                 :trap-station-latitude 40.713
                 :media-capture-timestamp (t/date-time 2016 6 1)
                 :percent-nocturnal "X"}]))))

    (testing "should assoc to '' if captured during the day"
      (let [data [{:trap-station-longitude -74.005
                   :trap-station-latitude 40.713
                   :media-capture-timestamp (t/date-time 2016 6 1 8 0 0)}]]
        (is (= (calculate data)
               [{:trap-station-longitude -74.005
                 :trap-station-latitude 40.713
                 :media-capture-timestamp (t/date-time 2016 6 1 8 0 0)
                 :percent-nocturnal ""}])))))

  (testing "aggregate-is-night"
    (testing "should aggregate based on percent-nocturnal flag"
      (let [data [{:percent-nocturnal "X"
                   :media-id 1
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 8 0 0)
                   :taxonomy-id 1
                   :sighting-quantity 2}
                  {:percent-nocturnal ""
                   :media-id 2
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 12 0 0)
                   :taxonomy-id 1
                   :sighting-quantity 1}]]
        (is (= (aggregate data) "66.67"))))

    (testing "should aggregate using initial sighting's nocturnal state"
      (let [data [{:percent-nocturnal "X"
                   :media-id 1
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 8 0 0)
                   :taxonomy-id 1
                   :sighting-quantity 2}
                  {:percent-nocturnal ""
                   :media-id 2
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 8 10 0)
                   :taxonomy-id 1
                   :sighting-quantity 1}]]
        (is (= (aggregate data) "100.00"))))

    (testing "should aggregate using initial sighting's nocturnal state"
      (let [data [{:percent-nocturnal "X"
                   :media-id 1
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 8 0 0)
                   :taxonomy-id 1
                   :sighting-quantity 2}
                  {:percent-nocturnal ""
                   :media-id 1
                   :trap-station-session-id 1
                   :media-capture-timestamp (t/date-time 2016 6 1 8 30 0)
                   :taxonomy-id 1
                   :sighting-quantity 1}]]
        (is (= (aggregate data) "66.67"))))))
