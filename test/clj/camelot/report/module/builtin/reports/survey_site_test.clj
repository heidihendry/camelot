(ns camelot.report.module.builtin.reports.survey-site-test
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
                camelot.model.survey-site/get-specific (constantly
                                                        {:survey-name "Survy"
                                                         :site-name "Sitey"})]
    (sut/report :survey-site-statistics state {:survey-site-id id} data)))

(defn csv-report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})
                camelot.model.survey-site/get-specific (constantly
                                                        {:survey-name "Survy"
                                                         :site-name "Sitey"})]
    (sut/csv-report :survey-site-statistics state {:survey-site-id id} data)))

(def headings ["Survey Name"
               "Site Name"
               "Genus"
               "Species"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Abundance Index"])

(deftest test-survey-site-report
  (testing "Survey site Report"
    (testing "Report data form empty sightings is empty"
      (let [sightings '()
            state (gen-state-helper {})
            result (report state 1 sightings)]
        (is (= result '()))))

    (testing "Media without sightings should be excluded"
      (let [sightings (list {:taxonomy-id nil
                             :sighting-quantity nil
                             :media-id nil
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-id 1
                             :survey-site-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Report with one sighting should contain its summary"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :media-id 1
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Should exclude sightings in other survey sites"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :media-id 2
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :survey-site-id 1
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 5
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :survey-site-id 2
                             :trap-station-session-id 2
                             :trap-station-id 2})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))))

    (testing "Should return a result per species even those not sighted at that location"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-id 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id nil
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id nil}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :sighting-quantity 1
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id 2
                             :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "A" "Meerkat" nil nil 7 nil]
                            ["Survy" "Sitey" "Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)]
                            ["Survy" "Sitey" "Yellow" "Spotted Cat" nil nil 7 nil])))))

    (testing "Should return a result per species where all are in the same site"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id 2}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :media-id 3
                             :sighting-quantity 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "A" "Meerkat" "X" 1 21 (calc-obs-nights 1 21)]
                            ["Survy" "Sitey" "Smiley" "Wolf" "X" 3 21 (calc-obs-nights 3 21)]
                            ["Survy" "Sitey" "Yellow" "Spotted Cat" "X" 5 21 (calc-obs-nights 5 21)])))))

    (testing "Should group multiple sightings from different camera traps"
      (let [sightings (list {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :sighting-quantity 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :media-id 1
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :media-id 2
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 2
                             :taxonomy-genus "Smiley"
                             :taxonomy-species "Wolf"
                             :media-id 3
                             :sighting-quantity 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Sitey" "Smiley" "Wolf" "X" 4 14 (calc-obs-nights 4 14)]
                            ["Survy" "Sitey" "Yellow" "Spotted Cat" "X" 5 14 (calc-obs-nights 5 14)]))))))

  (testing "Survey site report CSV output"
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
                             :media-id 3
                             :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                             :survey-site-id 1
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 1
                             :trap-station-id 1}
                            {:taxonomy-id 1
                             :taxonomy-genus "Yellow"
                             :taxonomy-species "Spotted Cat"
                             :sighting-quantity 5
                             :media-id 2
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id nil
                             :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                             :trap-station-session-id 2
                             :trap-station-id nil}
                            {:taxonomy-id 3
                             :taxonomy-genus "A"
                             :taxonomy-species "Meerkat"
                             :sighting-quantity 1
                             :media-id 1
                             :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                             :survey-site-id 2
                             :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                             :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                             :trap-station-session-id 3
                             :trap-station-id 3})
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (csv-report state 1 sightings)]
        (is (= result (str (str/join "," headings) "\n"
                           "Survy,Sitey,A,Meerkat,-,-,7,-\n"
                           "Survy,Sitey,Smiley,Wolf,X,3,7," (calc-obs-nights 3 7) "\n"
                           "Survy,Sitey,Yellow,Spotted Cat,-,-,7,-\n")))))))
