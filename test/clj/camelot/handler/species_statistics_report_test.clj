(ns camelot.handler.species-statistics-report-test
  (:require [camelot.handler.species-statistics-report :as sut]
            [midje.sweet :refer :all]
            [camelot.util.application :as app]
            [clj-time.core :as t]
            [clojure.string :as str]
            [camelot.handler.species :as species]))

(defn- gen-state-helper
  [config]
  (app/gen-state (assoc config :language :en)))

(defn- calc-obs-nights
  [obs nights]
  (format "%.3f" (* 100 (double (/ obs nights)))))


(def headings ["Species Scientific Name"
               "Trap Station Longitude"
               "Trap Station Latitude"
               "Presence"
               "Independent Observations"
               "Nights Elapsed"
               "Observations / Night (%)"])

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
          state (gen-state-helper {})
          result (sut/report state 1 sightings)]
      result => '()))

  (fact "Media without sightings should be excluded"
    (with-redefs [camelot.handler.species/get-specific
                  (fn [state id]
                    {:species-scientific-name "Smiley Wolf"})]
      (let [sightings (list (as-sample {})
                            (as-sample {:species-scientific-name "Smiley Wolf"
                                        :sighting-quantity 3
                                        :media-id 1
                                        :species-id 1
                                        :trap-station-session-id 1}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (sut/report state 1 sightings)]
        result => (list ["Smiley Wolf" 30 5 "X" 3 7 (calc-obs-nights 3 7)]))))

  (fact "Report for one sighting should contain its summary"
    (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 3
                                      :media-id 1
                                      :species-id 1
                                      :trap-station-session-id 1}))
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state 1 sightings)]
      result => (list ["Smiley Wolf" 30 5 "X" 3 7 (calc-obs-nights 3 7)])))

  (fact "Should return a record per location."
    (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 3
                                      :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                      :species-id 1
                                      :trap-station-id 1
                                      :trap-station-session-id 1})
                          (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 5
                                      :species-id 1
                                      :trap-station-longitude 30.5
                                      :trap-station-latitude 5.5
                                      :trap-station-id 2
                                      :media-capture-timestamp (t/date-time 2015 1 4 10 50 15)
                                      :trap-station-session-id 2}))
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/report state 1 sightings)]
      result => (list ["Smiley Wolf" 30 5 "X" 3 7 (calc-obs-nights 3 7)]
                      ["Smiley Wolf" 30.5 5.5 "X" 5 7 (calc-obs-nights 5 7)])))

  (fact "Should respect independence threshold setting"
    (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 3
                                      :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                      :species-id 1
                                      :trap-station-id 1
                                      :trap-station-session-id 1})
                          (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 5
                                      :trap-station-id 1
                                      :species-id 1
                                      :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                      :trap-station-session-id 1}))
          state (gen-state-helper {:sighting-independence-minutes-threshold 10})
          result (sut/report state 1 sightings)]
      result => (list ["Smiley Wolf" 30 5 "X" 8 7 (calc-obs-nights 8 7)])))

  (fact "Should include entries for locations the species was not found in"
    (with-redefs [camelot.handler.species/get-specific
                  (fn [state id]
                    {:species-scientific-name "Smiley Wolf"})]
      (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                        :sighting-quantity 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :species-id 1
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
            state (gen-state-helper {:sighting-independence-minutes-threshold 10})
            result (sut/report state 1 sightings)]
        result => (list ["Smiley Wolf" 30 5 "X" 3 7 (calc-obs-nights 3 7)]
                        ["Smiley Wolf" 40 10 nil nil 7 nil]
                        ["Smiley Wolf" 90 50 nil nil 7 nil]))))

  (fact "Should return only the species searched"
    (with-redefs [camelot.handler.species/get-specific
                  (fn [state id]
                    {:species-scientific-name "A. Meerkat"})]
      (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                        :sighting-quantity 3
                                        :species-id 1
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                        :trap-station-session-id 1})
                            (as-sample {:species-scientific-name "Yellow Spotted Cat"
                                        :sighting-quantity 5
                                        :species-id 2
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 2})
                            (as-sample {:species-scientific-name "A. Meerkat"
                                        :sighting-quantity 1
                                        :species-id 3
                                        :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                        :trap-station-session-id 3}))
            state (gen-state-helper {:sighting-independence-minutes-threshold 20})
            result (sut/report state 3 sightings)]
        result => (list ["A. Meerkat" 30 5 "X" 1 7 (calc-obs-nights 1 7)])))))

(facts "CSV output"
  (fact "CSV should contain header row"
    (let [sightings '()
          state (gen-state-helper {})
          result (sut/csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n")))

  (fact "Should return a record per location."
    (let [sightings (list (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 3
                                      :media-capture-timestamp (t/date-time 2015 1 3 10 10 15)
                                      :species-id 1
                                      :trap-station-id 1
                                      :trap-station-session-id 1})
                          (as-sample {:species-scientific-name "Smiley Wolf"
                                      :sighting-quantity 5
                                      :species-id 1
                                      :trap-station-longitude 30.5
                                      :trap-station-latitude 5.5
                                      :trap-station-id 2
                                      :media-capture-timestamp (t/date-time 2015 1 3 10 20 15)
                                      :trap-station-session-id 2}))
          state (gen-state-helper {:sighting-independence-minutes-threshold 20})
          result (sut/csv-report state 1 sightings)]
      result => (str (str/join "," headings) "\n"
                     "Smiley Wolf,30,5,X,3,7," (calc-obs-nights 3 7) "\n"
                     "Smiley Wolf,30.5,5.5,X,5,7," (calc-obs-nights 5 7) "\n"))))
