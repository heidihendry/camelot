(ns camelot.report.module.builtin.reports.trap-station-test
  (:require [camelot.test-util.state :as state]
            [camelot.report.core :as sut]
            [clj-time.core :as t]
            [clojure.string :as str]
            [midje.sweet :refer :all]))

(defn- gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(defn report
  [state id data]
  (sut/report :trap-station-statistics state {:trap-station-id id} data))

(defn csv-report
  [state id data]
  (sut/csv-report :trap-station-statistics state {:trap-station-id id} data))

(def headings ["Genus"
               "Species"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Abundance Index"])

(facts "Summary Statistics Report"
  (fact "Report data form empty sightings is empty"
    (let [sightings '()
          state (gen-state-helper {})
          result (report state 1 sightings)]
      result => '()))

  (fact "Media without sightings should be excluded"
    (let [sightings (list {:taxonomy-id nil
                           :sighting-quantity nil
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
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 (vec sightings))]
      result => (list ["Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Report with one sighting should contain its summary"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :media-id 1
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Should exclude sightings in other survey trap stations"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
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
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 2
                           :trap-station-id 2})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Should respect independence threshold setting"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 5
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 10})
          result (report state 1 sightings)]
      result => (list ["Smiley" "Wolf" "X" 8 7 (calc-obs-nights 8 7)])))

  (fact "Should return a result per species even those not sighted at that location"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
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
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["A" "Meerkat" nil nil 7 nil]
                      ["Smiley" "Wolf" "X" 3 7 (calc-obs-nights 3 7)]
                      ["Yellow" "Spotted Cat" nil nil 7 nil])))

  (fact "Should return a result per species where all are in the same trap station"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
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
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 2
                           :trap-station-id 1}
                          {:taxonomy-id 3
                           :taxonomy-genus "A"
                           :taxonomy-species "Meerkat"
                           :media-id 3
                           :sighting-quantity 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["A" "Meerkat" "X" 1 21 (calc-obs-nights 1 21)]
                      ["Smiley" "Wolf" "X" 3 21 (calc-obs-nights 3 21)]
                      ["Yellow" "Spotted Cat" "X" 5 21 (calc-obs-nights 5 21)])))

  (fact "Should group multiple sightings from different camera traps sessions"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
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
                           :media-id 2
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
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley" "Wolf" "X" 4 14 (calc-obs-nights 4 14)]
                      ["Yellow" "Spotted Cat" "X" 5 14 (calc-obs-nights 5 14)]))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (let [sightings '()
          state (gen-state-helper {})
          result (csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n")))

  (fact "Should return a result per species even those not sighted at that location"
    (let [sightings (list {:taxonomy-id 2
                           :taxonomy-genus "Smiley"
                           :taxonomy-species "Wolf"
                           :sighting-quantity 3
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
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n"
                      "A,Meerkat,-,-,7,-" "\n"
                      "Smiley,Wolf,X,3,7," (calc-obs-nights 3 7) "\n"
                      "Yellow,Spotted Cat,-,-,7,-\n"))))
