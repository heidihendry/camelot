(ns camelot.report.summary-statistics-test
  (:require [camelot.report.summary-statistics :as sut]
            [midje.sweet :refer :all]
            [camelot.util.application :as app]
            [clj-time.core :as t]
            [clojure.string :as str]))

(defn- gen-state-helper
  [config]
  (app/gen-state (assoc config :language :en)))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(def headings ["Species Scientific Name"
               "Number of Trap Stations"
               "Number of Photos"
               "Independent Observations"
               "Nights Elapsed"
               "Observations / Night (%)"])

(facts "Summary Statistics Report"
  (fact "Report data form empty sightings is empty"
    (let [sightings '()
          state (gen-state-helper {})
          result (sut/report state sightings)]
      result => '()))

  (fact "Media without sightings should be excluded"
    (let [sightings (list {:species-scientific-name nil
                           :sighting-quantity nil
                           :media-id nil
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 1 1 3 7 (calc-obs-nights 3 7)])))

  (fact "Report for one sighting should contain its summary"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :media-id 1
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 1 1 3 7 (calc-obs-nights 3 7)])))

  (fact "Should account for dependence in sightings"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :media-id 2
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 5
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 1 2 5 7 (calc-obs-nights 5 7)])))

  (fact "Should respect independence threshold setting"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 5
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 10})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 1 2 8 7 (calc-obs-nights 8 7)])))

  (fact "Should not consider sightings dependent across trap stations"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 5
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 2
                           :trap-station-id 2})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 2 2 8 14 (calc-obs-nights 8 14)])))

  (fact "Should return a result per species, sightings across different trap stations"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Yellow Spotted Cat"
                           :sighting-quantity 5
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 2
                           :trap-station-id 2}
                          {:species-scientific-name "A. Meerkat"
                           :sighting-quantity 1
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["A. Meerkat" 1 1 1 31 (calc-obs-nights 1 31)]
                      ["Smiley Wolf" 1 1 3 7 (calc-obs-nights 3 7)]
                      ["Yellow Spotted Cat" 1 1 5 7 (calc-obs-nights 5 7)])))

  (fact "Should return a result per species, sightings in same trap station session"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Yellow Spotted Cat"
                           :sighting-quantity 5
                           :media-id 2
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "A. Meerkat"
                           :media-id 3
                           :sighting-quantity 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["A. Meerkat" 1 1 1 7 (calc-obs-nights 1 7)]
                      ["Smiley Wolf" 1 1 3 7 (calc-obs-nights 3 7)]
                      ["Yellow Spotted Cat" 1 1 5 7 (calc-obs-nights 5 7)])))

  (fact "Should group multiple sightings from different camera traps"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :media-id 1
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Yellow Spotted Cat"
                           :sighting-quantity 5
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :media-id 2
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :media-id 3
                           :sighting-quantity 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state sightings)]
      result => (list ["Smiley Wolf" 2 2 4 14 (calc-obs-nights 4 14)]
                      ["Yellow Spotted Cat" 1 1 5 7 (calc-obs-nights 5 7)]))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (let [sightings '()
          state (gen-state-helper {})
          result (sut/csv-report state sightings)]
      result => (str (str/join "," headings) "\n")))

  (fact "Should group multiple sightings from different camera traps"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Yellow Spotted Cat"
                           :sighting-quantity 5
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :media-id 2
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1}
                          {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :media-id 3
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/csv-report state sightings)]
      result => (str (str/join "," headings) "\n"
                     "Smiley Wolf,2,2,4,14," (calc-obs-nights 4 14) "\n"
                     "Yellow Spotted Cat,1,1,5,7," (calc-obs-nights 5 7) "\n"))))
