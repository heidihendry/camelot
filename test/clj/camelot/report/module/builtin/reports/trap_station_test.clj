(ns camelot.report.module.builtin.reports.trap-station-test
  (:require
   [camelot.testutil.state :as state]
   [camelot.report.core :as sut]
   [clj-time.core :as t]
   [clojure.string :as str]
   [clojure.test :refer :all :exclude [report]]))

(defn- gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(defn- calc-obs-nights
  [^long obs ^long nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(defn report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})
                camelot.model.trap-station/get-specific (constantly
                                                         {:trap-station-name "Trapy"
                                                          :trap-station-id 1})]
    (sut/report :trap-station-statistics state {:trap-station-id id} data)))

(defn csv-report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})
                camelot.model.trap-station/get-specific (constantly
                                                        {:trap-station-id 1
                                                         :trap-station-name "Trapy"})]
    (sut/csv-report :trap-station-statistics state {:trap-station-id id} data)))

(def headings ["Trap Station ID"
               "Trap Station Name"
               "Genus"
               "Species"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Abundance Index"])

(deftest test-trap-station-report
  (testing "Trap Station report"
    (testing "Report data form empty sightings is empty"
      (let [sightings '()
            state (gen-state-helper {})
            result (report state 1 sightings)]
        (is (= result '()))))

    (testing "Media without sightings should be excluded"
      (let [sightings (list {:taxonomy-id nil
                             :sighting-quantity nil
                             :trap-station-name "Trapy"
                             :media-id nil
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :trap-station-name "Trapy"
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 (vec sightings))]
        (is (= result (list [1 "Trapy" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Report with one sighting should contain its summary"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-name "Trapy"
                             :media-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list [1 "Trapy" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Should exclude sightings in other survey trap stations"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :trap-station-name "Trapy"
                             :media-id 2
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 5
                             :trap-station-name "Other trap"
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id 2})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list [1 "Trapy" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Should return a result per species even those not sighted at that location"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :trap-station-name "Trapy"
                             :media-id 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :trap-station-name nil
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id nil
                             :trap-station-id nil}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :sighting-quantity 1
                             :trap-station-name "Other trap"
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list [1 "Trapy" "A" "Meerkat" nil nil 7 nil]
                            [1 "Trapy" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)]
                            [1 "Trapy" "Yellow" "Spotted Cat" nil nil 7 nil])))))

    (testing "Should return a result per species where all are in the same trap station"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :trap-station-name "Trapy"
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :trap-station-name "Trapy"
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id 1}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :trap-station-name "Trapy"
                             :media-id 3
                             :sighting-quantity 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list [1 "Trapy" "A" "Meerkat" "X" 1 21 (calc-obs-nights 1 21)]
                            [1 "Trapy" "Smiley" "Wolf" "X" 3 21 (calc-obs-nights 3 21)]
                            [1 "Trapy" "Yellow" "Spotted Cat" "X" 5 21 (calc-obs-nights 5 21)])))))

    (testing "Should group multiple sightings from different camera traps sessions"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-name "Trapy"
                             :media-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-name "Trapy"
                             :media-id 2
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :trap-station-name "Trapy"
                             :media-id 3
                             :sighting-quantity 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list [1 "Trapy" "Smiley" "Wolf" "X" 4 14 (calc-obs-nights 4 14)]
                            [1 "Trapy" "Yellow" "Spotted Cat" "X" 5 14 (calc-obs-nights 5 14)]))))))

  (testing "CSV output"
    (testing "CSV should contain header row"
      (let [sightings '()
            state (gen-state-helper {})
            result (csv-report state 1 sightings)]
        (is (= result (str (str/join "," headings) "\n")))))

    (testing "Should return a result per species even those not sighted at that location"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :trap-station-name "Trapy"
                             :media-id 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :trap-station-name nil
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id nil}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :sighting-quantity 1
                             :trap-station-name "Other trap"
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (csv-report state 1 sightings)]
        (is (= result (str (str/join "," headings) "\n"
                           "1,Trapy,A,Meerkat,-,-,7,-" "\n"
                           "1,Trapy,Smiley,Wolf,X,3,7," (calc-obs-nights 3 7) "\n"
                           "1,Trapy,Yellow,Spotted Cat,-,-,7,-\n")))))))
