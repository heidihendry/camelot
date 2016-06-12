(ns camelot.report.trap-station-test
  (:require [camelot.report-builder.core :as sut]
            [midje.sweet :refer :all]
            [camelot.application :as app]
            [clj-time.core :as t]
            [clojure.string :as str]))

(defn- gen-state-helper
  [config]
  (app/gen-state (assoc config :language :en)))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))

(def report
  (partial sut/report :trap-station-statistics))

(def csv-report
  (partial sut/csv-report :trap-station-statistics))

(def headings ["Species Scientific Name"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Observations / Night (%)"])

(facts "Summary Statistics Report"
  (fact "Report data form empty sightings is empty"
    (let [sightings '()
          state (gen-state-helper {})
          result (report state 1 sightings)]
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
          result (report state 1 sightings)]
      result => (list ["Smiley Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Report with one sighting should contain its summary"
    (let [sightings (list {:species-scientific-name "Smiley Wolf"
                           :sighting-quantity 3
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :media-id 1
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 1
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Should exclude sightings in other survey trap stations"
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
                           :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 2
                           :trap-station-id 2})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley Wolf" "X" 3 7 (calc-obs-nights 3 7)])))

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
          result (report state 1 sightings)]
      result => (list ["Smiley Wolf" "X" 8 7 (calc-obs-nights 8 7)])))

  (fact "Should return a result per species even those not sighted at that location"
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
                           :trap-station-session-id nil
                           :trap-station-id nil}
                          {:species-scientific-name "A. Meerkat"
                           :sighting-quantity 1
                           :media-id 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 28 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 2 28 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 3})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["A. Meerkat" nil nil 31 nil]
                      ["Smiley Wolf" "X" 3 7 (calc-obs-nights 3 7)]
                      ["Yellow Spotted Cat" nil nil 7 nil])))

  (fact "Should return a result per species where all are in the same trap station"
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
                           :trap-station-session-id 2
                           :trap-station-id 1}
                          {:species-scientific-name "A. Meerkat"
                           :media-id 3
                           :sighting-quantity 1
                           :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                           :trap-station-session-start-date (t/date-time 2015 1 1 0 0 0)
                           :trap-station-session-end-date (t/date-time 2015 1 8 0 0 0)
                           :trap-station-session-id 3
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["A. Meerkat" "X" 1 7 (calc-obs-nights 1 7)]
                      ["Smiley Wolf" "X" 3 7 (calc-obs-nights 3 7)]
                      ["Yellow Spotted Cat" "X" 5 7 (calc-obs-nights 5 7)])))

  (fact "Should group multiple sightings from different camera traps sessions"
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
                           :trap-station-id 1})
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (report state 1 sightings)]
      result => (list ["Smiley Wolf" "X" 4 14 (calc-obs-nights 4 14)]
                      ["Yellow Spotted Cat" "X" 5 7 (calc-obs-nights 5 7)]))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (let [sightings '()
          state (gen-state-helper {})
          result (csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n")))

  (fact "Should return a result per species even those not sighted at that location"
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
                           :trap-station-session-id nil
                           :trap-station-id nil}
                          {:species-scientific-name "A. Meerkat"
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
                      "A. Meerkat,,,31," "\n"
                      "Smiley Wolf,X,3,7," (calc-obs-nights 3 7) "\n"
                      "Yellow Spotted Cat,,,7,\n"))))
