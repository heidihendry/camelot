(ns camelot.report.module.builtin.reports.species-statistics-test
  (:require [camelot.application :as app]
            [camelot.report.core :as sut]
            [camelot.test-util.state :as state]
            [clj-time.core :as t]
            [clojure.string :as str]
            [midje.sweet :refer :all]
            [camelot.model.taxonomy :as taxonomy]))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(def headings ["Genus"
               "Species"
               "Trap Station Longitude"
               "Trap Station Latitude"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Abundance Index"])

(def report
  (partial sut/report :species-statistics))

(def csv-report
  (partial sut/csv-report :species-statistics))

(def default-sample
  {:media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
   :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
   :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
   :trap-station-id 1
   :site-id 1
   :trap-station-longitude 30
   :trap-station-latitude 5})

(defn as-sample
  [data]
  (merge default-sample data))

(facts "Species Statistics Report"
  (fact "Report data form empty sightings is empty"
    (let [sightings '()
          state (state/gen-state {})
          result (report state 1 sightings)]
      result => '()))

  (fact "Media without sightings should be excluded"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {})
                            (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-id 1
                                        :taxonomy-id 1
                                        :trap-station-session-id 1}))
            state (state/gen-state {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        result => (list ["Smiley" "Wolf" 30 5 "X" 3 14 (calc-obs-nights 3 14)]))))

  (fact "Report for one sighting should contain its summary"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-id 1
                                        :taxonomy-id 1
                                        :trap-station-session-id 1}))
            state (state/gen-state {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        result => (list ["Smiley" "Wolf" 30 5 "X" 3 7 (calc-obs-nights 3 7)]))))

  (fact "Should return a record per location."
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :taxonomy-id 1
                                        :trap-station-id 1
                                        :trap-station-session-id 1})
                            (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 5
                                        :taxonomy-id 1
                                        :trap-station-longitude 30.5
                                        :trap-station-latitude 5.5
                                        :trap-station-id 2
                                        :media-capture-timestamp (t/date-time 2015 1 4 10 50 15)
                                        :trap-station-session-id 2}))
            state (state/gen-state {:sighting-independence-minutes-threshold 20})
            result (report state 1 sightings)]
        result => (list ["Smiley" "Wolf" 30 5 "X" 3 14 (calc-obs-nights 3 14)]
                        ["Smiley" "Wolf" 30.5 5.5 "X" 5 14 (calc-obs-nights 5 14)]))))

  (fact "Should respect independence threshold setting"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :taxonomy-id 1
                                        :trap-station-id 1
                                        :trap-station-session-id 1})
                            (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 5
                                        :trap-station-id 1
                                        :taxonomy-id 1
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 1}))
            state (state/gen-state {:sighting-independence-minutes-threshold 10})
            result (report state 1 sightings)]
        result => (list ["Smiley" "Wolf" 30 5 "X" 8 7 (calc-obs-nights 8 7)]))))

  (fact "Should include entries for locations the species was not found in"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :taxonomy-id 1
                                        :trap-station-id 1
                                        :trap-station-session-id 1})
                            (as-sample {:trap-station-id 2
                                        :trap-station-longitude 40
                                        :trap-station-latitude 10
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 15 15)
                                        :trap-station-session-id 2})
                            (as-sample {:trap-station-id 3
                                        :trap-station-longitude 90
                                        :trap-station-latitude 50
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 30 15)
                                        :trap-station-session-id 3}))
            state (state/gen-state {:sighting-independence-minutes-threshold 10})
            result (report state 1 sightings)]
        result => (list ["Smiley" "Wolf" 30 5 "X" 3 21 (calc-obs-nights 3 21)]
                        ["Smiley" "Wolf" 40 10 nil nil 21 nil]
                        ["Smiley" "Wolf" 90 50 nil nil 21 nil]))))

  (fact "Should return only the species searched"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "A"
                     :taxonomy-species "Meerkat"
                     :taxonomy-id 3})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :taxonomy-id 1
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :trap-station-session-id 1})
                            (as-sample {:taxonomy-genus "Yellow"
                                        :taxonomy-species "Spotted Cat"
                                        :sighting-quantity 5
                                        :taxonomy-id 2
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 2})
                            (as-sample {:taxonomy-genus "A"
                                        :taxonomy-species "Meerkat"
                                        :sighting-quantity 1
                                        :taxonomy-id 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 3}))
            state (state/gen-state {:sighting-independence-minutes-threshold 20})
            result (report state 3 sightings)]
        result => (list ["A" "Meerkat" 30 5 "X" 1 21 (calc-obs-nights 1 21)])))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings '()
            state (state/gen-state {})
            result (csv-report state 1 sightings)]
        result => (str (str/join "," headings) "\n"))))

  (fact "Should return a record per location."
    (with-redefs [taxonomy/get-specific
                  (fn [state id]
                    {:taxonomy-genus "Smiley"
                     :taxonomy-species "Wolf"
                     :taxonomy-id 1})]
      (let [sightings (list (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :taxonomy-id 1
                                        :trap-station-id 1
                                        :trap-station-session-id 1})
                            (as-sample {:taxonomy-genus "Smiley"
                                        :taxonomy-species "Wolf"
                                        :sighting-quantity 5
                                        :taxonomy-id 1
                                        :trap-station-longitude 30.5
                                        :trap-station-latitude 5.5
                                        :trap-station-id 2
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 2}))
            state (state/gen-state {:sighting-independence-minutes-threshold 20})
            result (csv-report state 1 sightings)]
        result => (str (str/join "," headings) "\n"
                       "Smiley,Wolf,30,5,X,3,14," (calc-obs-nights 3 14) "\n"
                       "Smiley,Wolf,30.5,5.5,X,5,14," (calc-obs-nights 5 14) "\n")))))
