(ns camelot.report.module.builtin.columns.species-sighting-time-deltas-test
  (:require [camelot.report.module.builtin.columns.species-sighting-time-deltas :as sut]
            [midje.sweet :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en} config)))

(facts "Species sighting base time delta"
  (fact "Should calculate the time delta between two species sightings at the same trap station"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)
           (remove nil?)
           first
           (t/in-seconds)) => 600))

  (fact "Should not consider distinct traps eligible for time deltas"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 2
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)) => [nil nil]))

  (fact "Should not consider distinct species eligible for time deltas"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 2
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)) => [nil nil]))

  (fact "Should time delta should be based on the last sighting"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 35 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)
           (remove nil?)
           (map t/in-seconds)) => [600 1500]))

  (fact "Should resume time deltas after an interruption by something with a different taxonomy and trap ID."
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}
                {:trap-station-id 2
                 :taxonomy-id 2
                 :media-capture-timestamp (t/date-time 2015 1 10 5 20 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 35 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)
           (remove nil?)
           (map t/in-seconds)) => [600 1500]))

  (fact "Should not break if some records have a nil trap or taxonomy ID"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}
                {:trap-station-id nil
                 :taxonomy-id nil
                 :media-capture-timestamp nil}
                {:trap-station-id 2
                 :taxonomy-id 2
                 :media-capture-timestamp (t/date-time 2015 1 10 5 20 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 35 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)
           (remove nil?)
           (map t/in-seconds)) => [600 1500]))

  (fact "Copes with records which are out of order"
    (let [data [{:trap-station-id 2
                 :taxonomy-id 2
                 :media-capture-timestamp (t/date-time 2015 1 10 5 20 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 35 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}]]
      (->> data
           (sut/calculate-sighting-time-delta (gen-state-helper {}))
           (map :sighting-time-delta)
           (remove nil?)
           (map t/in-seconds)) => [600 1500])))

(facts "Species sighting base time delta representation colums"
  (fact "Should calculate time delta in seconds"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}]]
      (->> data
           (sut/calculate-time-delta-in-seconds (gen-state-helper {}))
           (map :sighting-time-delta-seconds)) => ["0" "600"]))

  (fact "Should calculate time delta in seconds"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}]]
      (->> data
           (sut/calculate-time-delta-in-minutes (gen-state-helper {}))
           (map :sighting-time-delta-minutes)) => ["0" "10"]))

  (fact "Should calculate time delta in hours, to 1dp"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 10 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 11 6 40 0)}]]
      (->> data
           (sut/calculate-time-delta-in-hours (gen-state-helper {}))
           (map :sighting-time-delta-hours)) => ["0.0" "0.2" "25.5"]))

  (fact "Should calculate time delta in days, to 1dp"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 11 7 30 0)}]]
      (->> data
           (sut/calculate-time-delta-in-days (gen-state-helper {}))
           (map :sighting-time-delta-days)) => ["0.0" "1.1"])))

(facts "Species sighting optimisations"
  (fact "Should only perform difference calculations once"
    (let [data [{:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 0 0)}
                {:trap-station-id 1
                 :taxonomy-id 1
                 :media-capture-timestamp (t/date-time 2015 1 10 5 30 0)}]
          processed (sut/calculate-time-delta-in-seconds (gen-state-helper {}) data)]
      (with-redefs [sut/sighting-time-delta-reducer
                    (fn [a c] (throw (RuntimeException. "Reducer called but should not be")))]
        (->> processed
             (sut/calculate-time-delta-in-minutes (gen-state-helper {}))
             (map :sighting-time-delta-minutes)) => ["0" "30"]))))
