(ns camelot.report.sighting-independence-test
  (:require [camelot.report.sighting-independence :as sut]
            [midje.sweet :refer :all]
            [clj-time.core :as t]
            [schema.test :as st]
            [camelot.application :as app]))

(defn gen-state-helper
  [config]
  (app/gen-state (assoc config :language :en)))

(namespace-state-changes (before :facts st/validate-schemas))

(def config
  {:infrared-iso-value-threshold 999
   :night-end-hour 5
   :night-start-hour 21
   :erroneous-infrared-threshold 0.5
   :sighting-independence-minutes-threshold 20})

(facts "Sighting Independence"
  (fact "A single sighting is extracted"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 1})))

  (fact "Multiple species are extracted if present"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 1}
                                                           {:species "Smiley Wolf"
                                                            :count 2})))

  (fact "Sightings are only considered independent if having sufficient temporal distance"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 3})))

  (fact "A sighting exactly on the threshold is independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 20 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 40 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 4})))

  (fact "A sighting may later need to be updated with a higher quantity"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 4})))

  (fact "A single sighting may contain multiple species"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 5}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                        :count 4}
                                                       {:species "Smiley Wolf"
                                                        :count 7})))

  (fact "Results are correct regardless of ordering of input"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 5}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 4}
                                                           {:species "Smiley Wolf"
                                                            :count 7}))))
