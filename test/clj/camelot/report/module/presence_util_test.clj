(ns camelot.report.module.presence-util-test
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

(defn generate-count
  [tax-id start-date end-date state data]
  (with-redefs [camelot.model.survey/survey-settings (constantly {})]
    (sut/generate-count tax-id start-date end-date state data)))

(defn generate-presence
  [tax-id start-date end-date state data]
  (with-redefs [camelot.model.survey/survey-settings (constantly {})]
    (sut/generate-presence tax-id start-date end-date state data)))

(defn- day-end
  [d]
  (t/minus (t/plus d (t/days 1)) (t/millis 1)))

(defn ->record
  [overrides]
  (merge {:taxonomy-id 1
          :trap-station-name "T1"
          :trap-station-id 1
          :trap-station-session-start-date (t/date-time 2015 1 1)
          :trap-station-session-end-date (t/date-time 2015 1 15)
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
            result (generate-count 1 (jan 1) (jan 4) (gen-state-helper {}) data)]
        (is (= (count result) 2))
        (is (= (count (first result)) 5))))

    (testing "Occupancy matrix shows sighting counts on correct days with all other results zero."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :sighting-quantity 3})]
            result (rest (generate-count 1 (jan 1) (jan 6) (gen-state-helper {}) data))]
        (is (= (get-in (mapv vec result) [0 3]) 1))
        (is (= (get-in (mapv vec result) [0 5]) 3))
        (is (= (check-non-zero result) [1 3]))))

    (testing "Occupancy matrix shows a row for each trap station."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 2})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 3})]
            result (generate-count 1 (jan 1) (jan 6) (gen-state-helper {}) data)]
        (is (= (map first result) ["", "T1", "T2", "T3"]))))

    (testing "Occupancy matrix does not show records outside of date range, but still shows trap stations."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 5)
                             :trap-station-id 2
                             :sighting-quantity 3})
                  (->record {:media-capture-timestamp (jan 7)
                             :trap-station-id 3})]
            result (generate-count 1 (jan 3) (jan 6) (gen-state-helper {}) data)]
        (is (= (map first result) ["", "T1", "T2", "T3"]))
        (is (= (check-non-zero (rest result)) [3]))))

    (testing "Other species are not included in results."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 3)
                             :taxonomy-id 2
                             :sighting-quantity 3})
                  (->record {:media-capture-timestamp (jan 5)})]
            result (generate-count 2 (jan 1) (jan 6) (gen-state-helper {}) data)]
        (is (= (check-non-zero (rest result)) [3]))))

    (testing "Results from the first and final days are included."
      (let [data [(->record {:media-capture-timestamp (jan 1)})
                  (->record {:media-capture-timestamp (jan 5)})]
            result (generate-count 1 (jan 1) (jan 5) (gen-state-helper {}) data)]
        (is (= (get-in (mapv vec result) [1 1]) 1))
        (is (= (get-in (mapv vec result) [1 5]) 1))))))

(deftest test-generate-presence
  (testing "Generate presence"
    (testing "Should show only 1/0 for presence/absence."
      (let [data [(->record {:media-capture-timestamp (jan 3)})
                  (->record {:media-capture-timestamp (jan 5)
                             :sighting-quantity 3})]
            result (rest (generate-presence 1 (jan 1) (jan 6) (gen-state-helper {}) data))]
        (is (= (get-in (mapv vec result) [0 3]) 1))
        (is (= (get-in (mapv vec result) [0 5]) 1))
        (is (= (check-non-zero result) [1 1]))))

    (testing "Should show hyphen for dates outside of session dates."
      (let [data [(->record {:media-capture-timestamp (jan 14)})
                  (->record {:media-capture-timestamp (jan 15)
                             :sighting-quantity 3})]
            result (rest (generate-presence 1 (jan 14) (jan 17) (gen-state-helper {}) data))]
        (is (= (rest (first (mapv vec result))) [1 1 "-" "-"])))))

  (testing "Date calculations shuold be distinct per trap station"
    (let [data [(->record {:media-capture-timestamp (jan 14)})
                (->record {:media-capture-timestamp (jan 15)
                           :sighting-quantity 3})
                (->record {:media-capture-timestamp (jan 17)
                           :trap-station-id 2
                           :trap-station-session-start-date (t/date-time 2015 1 17)
                           :trap-station-session-end-date (t/date-time 2015 1 20)
                           :sighting-quantity 3})
                (->record {:media-capture-timestamp (jan 20)
                           :trap-station-id 2
                           :trap-station-session-start-date (t/date-time 2015 1 17)
                           :trap-station-session-end-date (t/date-time 2015 1 20)
                           :sighting-quantity 3})]
          result (rest (generate-presence 1 (jan 14) (jan 17) (gen-state-helper {}) data))]
      (is (= (map rest (mapv vec result)) [[1 1 "-" "-"]
                                           ["-" "-" "-" 1]]))))

  (testing "Should shows hyphen for all, if no sessions within nominated dates."
    (let [data [(->record {:media-capture-timestamp (jan 14)})
                (->record {:media-capture-timestamp (jan 15)
                           :sighting-quantity 3})]
          result (rest (generate-presence 1 (jan 20) (jan 23) (gen-state-helper {}) data))]
      (is (= (rest (first (mapv vec result))) ["-" "-" "-" "-"])))))

(deftest test-session-date-ranges
  (testing "Session date ranges"
    (testing "Computes range for zero entries"
      (is (= (sut/session-date-ranges []) [])))

    (testing "Computes range for one entry"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 7)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 7))]]))))

    (testing "Computes range for non-overlapping entries"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 7)}
                  {:trap-station-session-start-date (t/date-time 2015 1 9)
                   :trap-station-session-end-date (t/date-time 2015 1 15)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 7))]
                [(t/date-time 2015 1 9)
                 (day-end (t/date-time 2015 1 15))]]))))

    (testing "Date ranges for adjacent periods are merged"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 8)}
                  {:trap-station-session-start-date (t/date-time 2015 1 9)
                   :trap-station-session-end-date (t/date-time 2015 1 15)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 15))]]))))

    (testing "Computes range for overlapping entries"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 7)}
                  {:trap-station-session-start-date (t/date-time 2015 1 6)
                   :trap-station-session-end-date (t/date-time 2015 1 15)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 15))]]))))

    (testing "Computes range for mixed overlapping and non-overlapping entries"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 7)}
                  {:trap-station-session-start-date (t/date-time 2015 1 6)
                   :trap-station-session-end-date (t/date-time 2015 1 15)}
                  {:trap-station-session-start-date (t/date-time 2015 1 17)
                   :trap-station-session-end-date (t/date-time 2015 1 20)}
                  {:trap-station-session-start-date (t/date-time 2015 1 18)
                   :trap-station-session-end-date (t/date-time 2015 1 21)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 15))]
                [(t/date-time 2015 1 17)
                 (day-end (t/date-time 2015 1 21))]]))))

    (testing "Computes range for mixed overlapping and non-overlapping entries, regardless of order"
      (let [data [{:trap-station-session-start-date (t/date-time 2015 1 18)
                   :trap-station-session-end-date (t/date-time 2015 1 21)}
                  {:trap-station-session-start-date (t/date-time 2015 1 1)
                   :trap-station-session-end-date (t/date-time 2015 1 7)}
                  {:trap-station-session-start-date (t/date-time 2015 1 17)
                   :trap-station-session-end-date (t/date-time 2015 1 20)}
                  {:trap-station-session-start-date (t/date-time 2015 1 6)
                   :trap-station-session-end-date (t/date-time 2015 1 15)}]]
        (is (= (sut/session-date-ranges data)
               [[(t/date-time 2015 1 1)
                 (day-end (t/date-time 2015 1 15))]
                [(t/date-time 2015 1 17)
                 (day-end (t/date-time 2015 1 21))]]))))))
