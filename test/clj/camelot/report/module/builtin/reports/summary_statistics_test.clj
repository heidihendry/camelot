(ns camelot.report.module.builtin.reports.summary-statistics-test
  (:require [camelot.test-util.state :as state]
            [camelot.report.core :as sut]
            [clj-time.core :as t]
            [clojure.string :as str]
            [midje.sweet :refer :all]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en
                         :timezone "Asia/Ho_Chi_Minh"}
                        config)))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(def report
  (partial sut/report :summary-statistics))

(def csv-report
  (partial sut/csv-report :summary-statistics))

(def headings ["Genus"
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

(facts "Summary Statistics Report"
  (fact "Report data form empty sightings is empty"
    (let [sightings '()
          state (gen-state-helper {})
          result (report state 1 sightings)]
      result => '()))

  (fact "Media without sightings should be excluded"
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
      result => (list ["Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)])))

  (fact "Report for one sighting should contain its summary"
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
      result => (list ["Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)])))

  (fact "Should account for dependence in sightings"
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
      result => (list ["Smiley" "Wolf" 1 2 5 "0.00" 7 (calc-obs-nights 5 7)])))

  (fact "Should respect independence threshold setting"
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
      result => (list ["Smiley" "Wolf" 1 2 8 "0.00" 7 (calc-obs-nights 8 7)])))

  (fact "Should not consider sightings dependent across trap stations"
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
      result => (list ["Smiley" "Wolf" 2 2 8 "0.00" 14 (calc-obs-nights 8 14)])))

  (fact "Should return a result per species, sightings across different trap stations"
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
      result => (list ["A" "Meerkat" 1 1 1 "0.00" 45 (calc-obs-nights 1 45)]
                      ["Smiley" "Wolf" 1 1 3 "0.00" 45 (calc-obs-nights 3 45)]
                      ["Yellow" "Spotted Cat" 1 1 5 "0.00" 45 (calc-obs-nights 5 45)])))

  (fact "Should return a result per species, sightings in same trap station session"
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
      result => (list ["A" "Meerkat" 1 1 1 "0.00" 7 (calc-obs-nights 1 7)]
                      ["Smiley" "Wolf" 1 1 3 "0.00" 7 (calc-obs-nights 3 7)]
                      ["Yellow" "Spotted Cat" 1 1 5 "0.00" 7 (calc-obs-nights 5 7)])))

  (fact "Should include trap session dates for stations without sightings"
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
      result => (list ["A" "Meerkat" 1 1 1 "0.00" 51 (calc-obs-nights 1 51)]
                      ["Yellow" "Spotted Cat" 1 1 5 "0.00" 51 (calc-obs-nights 5 51)])))

  (fact "Should return only details for the species for the given survey ID"
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
      result => (list ["A" "Meerkat" 1 1 1 "0.00" 7 (calc-obs-nights 1 7)]
                      ["Smiley" "Wolf" 0 0 0 nil 7 (calc-obs-nights 0 7)]
                      ["Yellow" "Spotted Cat" 0 0 0 nil 7 (calc-obs-nights 0 7)])))

  (fact "Should group multiple sightings from different camera traps"
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
      result => (list ["Smiley" "Wolf" 2 2 4 "0.00" 14 (calc-obs-nights 4 14)]
                      ["Yellow" "Spotted Cat" 1 1 5 "0.00" 14 (calc-obs-nights 5 14)])))

  (fact "Should calculate percentage of nocturnal sightings"
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
      result => (list ["Smiley" "Wolf" 2 2 4 "25.00" 14 (calc-obs-nights 4 14)]
                      ["Yellow" "Spotted Cat" 1 1 5 "100.00" 14 (calc-obs-nights 5 14)])))

  (fact "Should calculate nocturnal sightings based on all sightings in media"
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
                                     :media-capture-timestamp (t/date-time 2015 1 3 23 20 15)
                                     :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                                     :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                                     :trap-station-session-id 3
                                     :trap-station-id 3}))
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley" "Wolf" 2 2 4 "25.00" 14 (calc-obs-nights 4 14)]
                      ["Yellow" "Spotted Cat" 1 1 5 "100.00" 14 (calc-obs-nights 5 14)]))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (let [sightings '()
          state (gen-state-helper {})
          result (csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n")))

  (fact "Should group multiple sightings from different camera traps"
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
      result => (str (str/join "," headings) "\n"
                     "Smiley,Wolf,2,2,4,0.00,14," (calc-obs-nights 4 14) "\n"
                     "Yellow,Spotted Cat,1,1,5,0.00,14," (calc-obs-nights 5 14) "\n"))))
