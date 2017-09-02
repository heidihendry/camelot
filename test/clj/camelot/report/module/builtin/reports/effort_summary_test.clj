(ns camelot.report.module.builtin.reports.effort-summary-test
  (:require
   [camelot.report.core :as sut]
   [clojure.test :refer :all :exclude [report]]
   [camelot.test-util.state :as state]
   [clj-time.core :as t]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:timezone "Asia/Ho_Chi_Minh"
                           :language :en}
                        config)))

(defn report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])]
    (sut/report :effort-summary state {:survey-id id} data)))

(defn csv-report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])]
    (sut/csv-report :effort-summary state {:survey-id id} data)))

(def headings ["Site ID"
               "Site Name"
               "Site Area"
               "Number of Trap Stations"
               "Nights Elapsed"
               "Species Count"
               "Time Period Start"
               "Time Period End"])

(def default-record
  {:site-id 1
   :site-name "ASite"
   :site-area 1
   :survey-name "Survéy"
   :trap-station-id 1
   :trap-station-session-start-date (t/date-time 2015 1 1 10 10 10)
   :trap-station-session-end-date (t/date-time 2015 1 10 5 0 0)
   :trap-station-session-id 1
   :survey-id 1
   :survey-site-id 1
   :media-id 1
   :taxonomy-id nil
   :sighting-id nil
   })

(defn ->record
  [r]
  (merge default-record r))

(deftest test-effort-summary-report
  (testing "Effort Summary Report"
    (testing "Report data without records is empty"
      (let [records '()
            state (gen-state-helper {})
            result (report state 1 records)]
        (is (= result '()))))

    (testing "Report returns expected result for 1 site in the survey"
      (let [records (list
                     (->record {}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 1 9 0 "2015-01-01" "2015-01-10"]]))))

    (testing "Does not return a result should the survey not match"
      (let [records (list
                     (->record {:survey-id 2}))
            state (gen-state-helper {})]
        (is (= (report state 1 records) []))))

    (testing "Should return 1 species, should there be only 1 distinct species"
      (let [records (list
                     (->record {:taxonomy-id 1})
                     (->record {:taxonomy-id 1}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 1 9 1 "2015-01-01" "2015-01-10"]]))))

    (testing "Should return 2 species, should there be 2 distinct species"
      (let [records (list
                     (->record {:taxonomy-id 1})
                     (->record {:taxonomy-id 2}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 1 9 2 "2015-01-01" "2015-01-10"]]))))

    (testing "Should return 2 trap stations, should there be 2 distinct trap stations"
      (let [records (list
                     (->record {:trap-station-id 1})
                     (->record {:trap-station-id 2}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 2 9 0 "2015-01-01" "2015-01-10"]]))))

    (testing "Should return details for each site."
      (let [records (list
                     (->record {:site-id 1
                                :site-area 10})
                     (->record {:site-id 2
                                :site-name "Another site"
                                :site-area 50}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 10 1 9 0 "2015-01-01" "2015-01-10"]
                ["Survéy" 2 "Another site" 50 1 9 0 "2015-01-01" "2015-01-10"]]))))

    (testing "Should return earliest session month for a site as the start period."
      (let [records (list
                     (->record {:site-id 1})
                     (->record {:site-id 2
                                :trap-station-session-id 1
                                :trap-station-session-start-date (t/date-time 2014 8 15 1 1 1)})
                     (->record {:site-id 2
                                :trap-station-session-id 2
                                :trap-station-session-start-date (t/date-time 2014 5 15 1 1 1)})
                     (->record {:site-id 2
                                :trap-station-session-id 3
                                :trap-station-session-start-date (t/date-time 2014 7 15 1 1 1)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 1 9 0 "2015-01-01" "2015-01-10"]
                ["Survéy" 2 "ASite" 1 1 428 0 "2014-05-15" "2015-01-10"]]))))

    (testing "Should return lastest session month for a site as the end period."
      (let [records (list
                     (->record {:site-id 1})
                     (->record {:site-id 2
                                :trap-station-session-id 1
                                :trap-station-session-end-date (t/date-time 2016 1 5 1 1 1)})
                     (->record {:site-id 2
                                :trap-station-session-id 2
                                :trap-station-session-end-date (t/date-time 2016 5 30 1 1 1)})
                     (->record {:site-id 2
                                :trap-station-session-id 3
                                :trap-station-session-end-date (t/date-time 2016 2 28 1 1 1)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Survéy" 1 "ASite" 1 1 9 0 "2015-01-01" "2015-01-10"]
                ["Survéy" 2 "ASite" 1 1 947 0 "2015-01-01" "2016-05-30"]]))))))
