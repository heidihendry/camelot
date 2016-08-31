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

(facts "Sighting Independence extraction"
  (fact "A single sighting is extracted"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Multiple species are extracted if present"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1}
                                                               {:species-id 2
                                                                :count 2})))

  (fact "Sightings are only considered independent if having sufficient temporal distance"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 3})))

  (fact "A sighting exactly on the threshold is independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 20 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 40 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 4})))

  (fact "A sighting may later need to be updated with a higher quantity"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 4})))

  (fact "A single sighting may contain multiple species"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 5}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 4}
                                                               {:species-id 2
                                                                :count 7})))

  (fact "Results are correct regardless of ordering of input"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-quantity 2}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 5}
                     {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                      :taxonomy-id 2
                      :taxonomy-genus "Smiley"
                      :taxonomy-species "Wolf"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 4}
                                                               {:species-id 2
                                                                :count 7}))))

(facts "Sighting species sex"
  (fact "Sightings of a different sex are considered independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "F"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings of the same sex are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of unidentified and a single sex are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of sightings with an unidentified sex are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "unidentified"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of sightings with an unidentified sex are considered dependent for both male and female sightings"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "F"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "unidentified"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings without a sex are treated the same as an undefined sex"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "F"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex nil
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings without a sex are treated the same as an undefined sex"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "F"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex nil
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sex of a sighting can be inferred from later sightings"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-sex "F"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2}))))

(facts "Sighting species lifestage"
  (fact "Sightings of a different lifestage are considered independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings of the same lifestage are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of unidentified and a single lifestage are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of sightings with an unidentified lifestage are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "unidentified"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1})))

  (fact "Sightings of sightings with an unidentified lifestage are considered dependent for both adult and juvenile sightings"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "unidentified"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings without a lifestage are treated the same as an undefined lifestage"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage nil
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings without a lifestage are treated the same as an undefined lifestage"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage nil
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Lifestage of a sighting can be inferred from later sightings"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "unidentified"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2}))))

(facts "Sightings with mixed lifestage and sex"
  (fact "Sightings with a distinct lifestage but the same sex are considered independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Adult"
                      :sighting-sex "M"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-sex "M"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings with a distinct sex but the same lifestage are considered independent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-sex "F"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-sex "M"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 2})))

  (fact "Sightings with a the same sex and lifestage are considered dependent"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-sex "F"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :taxonomy-id 1
                      :taxonomy-genus "Yellow"
                      :taxonomy-species "Spotted Housecat"
                      :sighting-lifestage "Juvenile"
                      :sighting-sex "F"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (sut/extract-independent-sightings state sightings) => '({:species-id 1
                                                                :count 1}))))

(facts "Sighting independence"
  (fact "Records without sightings are excluded"
    (let [record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                  :taxonomy-id nil
                  :sighting-quantity nil
                  :media-id 1}
          state (gen-state-helper config)]
      (sut/->independent-sightings state record) => []))

  (fact "Records with sightings are included and sorted by date"
    (let [records (list {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                         :taxonomy-id 1
                         :sighting-quantity 1
                         :sighting-sex nil
                         :sighting-lifestage nil
                         :media-id 1}
                        {:media-capture-timestamp (t/date-time 2015 1 1 7 15 00)
                         :taxonomy-id 1
                         :sighting-quantity 3
                         :sighting-sex nil
                         :sighting-lifestage nil
                         :media-id 2}
                        {:media-capture-timestamp (t/date-time 2015 1 1 7 9 00)
                         :taxonomy-id 2
                         :sighting-quantity 2
                         :sighting-sex nil
                         :sighting-lifestage nil
                         :media-id 3})
          state (gen-state-helper config)]
      (sut/->independent-sightings state records) => [{:media-capture-timestamp (t/date-time 2015 1 1 7 9 00)
                                                       :sighting-independence-window-end (t/date-time 2015 1 1 7 29 00)
                                                       :taxonomy-id 2
                                                       :sighting-quantity 2
                                                       :sighting-sex nil
                                                       :sighting-lifestage nil
                                                       :media-id 3}
                                                      {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                                       :sighting-independence-window-end (t/date-time 2015 1 1 7 30 00)
                                                       :taxonomy-id 1
                                                       :sighting-quantity 3
                                                       :sighting-sex nil
                                                       :sighting-lifestage nil
                                                       :media-id 1}])))
