(ns camelot.report.module.persence-util-test
  (:require
   [camelot.report.module.presence-util :as sut]
   [clojure.test :refer :all]
   [camelot.test-util.state :as state]
   [clj-time.core :as t]))

(defn- jan
  [day]
  (t/date-time 2015 1 day))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:timezone "Asia/Ho_Chi_Minh"
                           :language :en}
                          config)))

(defn ->record
  [overrides]
  (merge {:taxonomy-id 1
          :trap-station-name "T1"
          :trap-station-id 1
          :trap-station-session-id 1
          :sighting-quantity 1}
         overrides
         (if (:trap-station-id overrides)
           {:trap-station-name (str "T" (:trap-station-id overrides))
            :trap-station-session-id (:trap-station-id overrides)}
           {})))

(defn check-non-zero
  [data]
  (->> data
       (map rest)
       flatten
       (remove #(= % 0))))

(deftest test-generate-count
  (testing "Occupancy matrix"
    (testing "Produces basic occupancy matrix with the correct dimensions"
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)})]
            result (sut/generate-count 1 (jan 1) (jan 4) (gen-state-helper {}) data)]
        (is (= (count result) 2))
        (is (= (count (first result)) 5))))

    (testing "Occupancy matrix shows sighting counts on correct days with all other results zero."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :sighting-quantity 3})]
            result (rest (sut/generate-count 1 (jan 1) (jan 6) (gen-state-helper {}) data))]
        (is (= (get-in (mapv vec result) [0 3]) 1))
        (is (= (get-in (mapv vec result) [0 5]) 3))
        (is (= (check-non-zero result) [1 3]))))

    (testing "Occupancy matrix shows a row for each trap station."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 2})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 3})]
            result (sut/generate-count 1 (jan 1) (jan 6) (gen-state-helper {}) data)]
        (is (= (map first result) ["", "T1", "T2", "T3"]))))

    (testing "Occupancy matrix does not show records outside of date range, but still shows trap stations."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 2
                             :sighting-quantity 3})
                  (->record {:media-capture-timestamp (jan 7)
                             :trap-station-id 3})]
            result (sut/generate-count 1 (jan 3) (jan 6) (gen-state-helper {}) data)]
        (is (= (map first result) ["", "T1", "T2", "T3"]))
        (is (= (check-non-zero (rest result)) [3]))))

    (testing "Other species are not included in results."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 3)
                             :taxonomy-id 2
                             :sighting-quantity 3})
                  (->record {:media-capture-timestamp (jan 5)})]
            result (sut/generate-count 2 (jan 1) (jan 6) (gen-state-helper {}) data)]
        (is (= (check-non-zero (rest result)) [3]))))

    (testing "Results from the first and final days are included."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 5)})]
            result (sut/generate-count 1 (jan 1) (jan 5) (gen-state-helper {}) data)]
        (is (= (get-in (mapv vec result) [1 1]) 1))
        (is (= (get-in (mapv vec result) [1 5]) 1))))

    (testing "Should show only 1/0 for presence/absence."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :sighting-quantity 3})]
            result (rest (sut/generate-presence 1 (jan 1) (jan 6) (gen-state-helper {}) data))]
        (is (= (get-in (mapv vec result) [0 3]) 1))
        (is (= (get-in (mapv vec result) [0 5]) 1))
        (is (= (check-non-zero result) [1 1]))))))
