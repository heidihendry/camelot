(ns camelot.report.module.builtin.reports.summary-statistics-test
  (:require
   [camelot.test-util.state :as state]
   [camelot.report.core :as sut]
   [clj-time.core :as t]
   [clojure.string :as str]
   [clojure.test :refer :all :exclude [report]]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en
                           :timezone "Asia/Ho_Chi_Minh"}
                        config)))

(defn- calc-obs-nights
  [^long obs ^long nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(defn report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})
                camelot.model.survey/get-specific (constantly {:survey-name "Survy"})]
    (sut/report :summary-statistics state {:survey-id id} data)))

(defn csv-report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})
                camelot.model.survey/get-specific (constantly {:survey-name "Survy"})]
    (sut/csv-report :summary-statistics state {:survey-id id} data)))

(def headings ["Survey Name"
               "Genus"
               "Species"
               "Number of Trap Stations"
               "Number of Photos"
               "Independent Observations"
               "Nocturnal (%)"
               "Nights Elapsed"
               "Abundance Index"])

(def default-record
  {:taxonomy-id nil
   :sighting-quantity nil
   :media-id nil
   :survey-id 1
   :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
   :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
   :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
   :trap-station-longitude "105.0"
   :trap-station-latitude "20.0"
   :trap-station-session-id 1
   :trap-station-id 1})

(defn ->record
  [data]
  (merge default-record data))

(deftest test-summary-statistics-report
  (testing "Summary Statistics Report"
    (testing "Report data form empty sightings is empty"
      (let [sightings '()
            state (gen-state-helper {})
            result (report state 1 sightings)]
        (is (= result '()))))

    (testing "Media without sightings should be excluded"
      (let [sightings (list (->record {})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 1
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)])))))

    (testing "Report for one sighting should contain its summary"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :media-id 1
                                       :survey-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)])))))

    (testing "Should account for dependence in sightings"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :media-id 2
                                       :sighting-quantity 3
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 5
                                       :media-id 1
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 1 2 5 "0.00" 7 (calc-obs-nights 5 7)])))))

    (testing "Should respect independence threshold setting"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 2
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 5
                                       :media-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :survey-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 10})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 1 2 8 "0.00" 7 (calc-obs-nights 8 7)])))))

    (testing "Should not consider sightings dependent across trap stations"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 1
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 5
                                       :media-id 2
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 2
                                       :trap-station-id 2}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 2 8 "0.00" 14 (calc-obs-nights 8 14)])))))

    (testing "Should return a result per species, sightings across different trap stations"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 3
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-id 2
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 2
                                       :trap-station-id 2})
                            (->record {:taxonomy-id 3
                                       :taxonomy-genus "A"
                                       :taxonomy-species "Meerkat"
                                       :sighting-quantity 1
                                       :media-id 1
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "A" "Meerkat" 1 1 1 "0.00" 45 (calc-obs-nights 1 45)]
                            ["Survy" "Smiley" "Wolf" 1 1 3 "0.00" 45 (calc-obs-nights 3 45)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 5 "0.00" 45 (calc-obs-nights 5 45)])))))

    (testing "Should return a result per species, sightings in same trap station session"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 1
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-id 2
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 3
                                       :taxonomy-genus "A"
                                       :taxonomy-species "Meerkat"
                                       :media-id 3
                                       :survey-id 1
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "A" "Meerkat" 1 1 1 "0.00" 7 (calc-obs-nights 1 7)]
                            ["Survy" "Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 5 "0.00" 7 (calc-obs-nights 5 7)])))))

    (testing "Should include trap session dates for stations without sightings"
      (let [sightings (list (->record {:species-scientific-name nil
                                       :sighting-quantity nil
                                       :media-id nil
                                       :survey-id 1
                                       :media-capture-timestamp nil
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:species-scientific-name nil
                                       :sighting-quantity nil
                                       :media-id nil
                                       :survey-id 1
                                       :media-capture-timestamp nil
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 31 0 0 0)
                                       :trap-station-session-id 4
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-id 2
                                       :survey-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 2
                                       :trap-station-id 2})
                            (->record {:taxonomy-id 3
                                       :taxonomy-genus "A"
                                       :taxonomy-species "Meerkat"
                                       :media-id 3
                                       :survey-id 1
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "A" "Meerkat" 1 1 1 "0.00" 51 (calc-obs-nights 1 51)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 5 "0.00" 51 (calc-obs-nights 5 51)])))))

    (testing "Should return only details for the species for the given survey ID"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-id 1
                                       :survey-id 3
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-id 2
                                       :survey-id 2
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 3
                                       :taxonomy-genus "A"
                                       :taxonomy-species "Meerkat"
                                       :media-id 3
                                       :survey-id 1
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "A" "Meerkat" 1 1 1 "0.00" 7 (calc-obs-nights 1 7)]
                            ["Survy" "Smiley" "Wolf" 0 0 0 nil 7 (calc-obs-nights 0 7)]
                            ["Survy" "Yellow" "Spotted Cat" 0 0 0 nil 7 (calc-obs-nights 0 7)])))))

    (testing "Should group multiple sightings from different camera traps"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :survey-id 1
                                       :media-id 2
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 3
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 2 4 "0.00" 14 (calc-obs-nights 4 14)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 5 "0.00" 14 (calc-obs-nights 5 14)])))))

    (testing "Should calculate percentage of nocturnal sightings"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-capture-timestamp (t/date-time 2015 1 3 1 20 15)
                                       :survey-id 1
                                       :media-id 2
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 3
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 23 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 2 4 "25.00" 14 (calc-obs-nights 4 14)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 5 "100.00" 14 (calc-obs-nights 5 14)])))))

    (testing "Dependent sightings which start at night should be classified as a night sighting."
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 45 15)
                                       :survey-id 1
                                       :media-id 3
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 2
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 23 20 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 3 2 "100.00" 14 (calc-obs-nights 2 14)])))))

    (testing "The number of sightings in a dependent sighting should affect the quantity."
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 9
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 45 15)
                                       :survey-id 1
                                       :media-id 3
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 2
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 00 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 3 10 "90.00" 14 (calc-obs-nights 10 14)])))))

    (testing "Media with two different sightings should be calculated correctly."
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-id 2
                                       :sighting-quantity 2
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 2
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 00 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 2 2 "50.00" 14 (calc-obs-nights 2 14)]
                            ["Survy" "Yellow" "Spotted Cat" 1 1 2 "100.00" 14 (calc-obs-nights 2 14)])))))

    (testing "Media from separate trap stations sessions are not to be considered dependent for nocturnal calculations."
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 2
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 50 00)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 2}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 2 2 "50.00" 14 (calc-obs-nights 2 14)])))))

    (testing "Independent sighting figures must not be counted twice."
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 6 30 15)
                                       :survey-id 1
                                       :media-id 1
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 2
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 8 50 00)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 2})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :survey-id 1
                                       :media-id 3
                                       :sighting-quantity 2
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 0 00)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 2}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        (is (= result (list ["Survy" "Smiley" "Wolf" 2 3 4 "25.00" 14 (calc-obs-nights 4 14)]))))))

  (testing "CSV output"
    (testing "CSV should contain header row"
      (let [sightings '()
            state (gen-state-helper {})
            result (csv-report state 1 sightings)]
        (is (= result (str (str/join "," headings) "\n")))))

    (testing "Should group multiple sightings from different camera traps"
      (let [sightings (list (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 3
                                       :survey-id 1
                                       :media-id 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 1
                                       :taxonomy-genus "Yellow"
                                       :taxonomy-species "Spotted Cat"
                                       :sighting-quantity 5
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :survey-id 1
                                       :media-id 2
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 1
                                       :trap-station-id 1})
                            (->record {:taxonomy-id 2
                                       :taxonomy-genus "Smiley"
                                       :taxonomy-species "Wolf"
                                       :sighting-quantity 1
                                       :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                       :survey-id 1
                                       :media-id 3
                                       :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                       :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                       :trap-station-session-id 3
                                       :trap-station-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (csv-report state 1 sightings)]
        (is (= result (str (str/join "," headings) "\n"
                           "Survy,Smiley,Wolf,2,2,4,0.00,14," (calc-obs-nights 4 14) "\n"
                           "Survy,Yellow,Spotted Cat,1,1,5,0.00,14," (calc-obs-nights 5 14) "\n")))))))
