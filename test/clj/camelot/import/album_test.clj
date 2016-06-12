(ns camelot.import.album-test
  (:require [camelot.fixtures.exif-test-metadata :refer :all]
            [camelot.import.album :refer :all]
            [clj-time.core :as t]
            [clojure.data :refer [diff]]
            [midje.sweet :refer :all]
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

(def sightings {:datetime (t/date-time 2015 1 1 0 0 0)
                :sightings [{:species "Smiley Wolf"
                             :quantity 3}]})
(def camera {:make "CamMaker" :model "MyCam"})
(def chrono-first {:datetime (t/date-time 2015 1 1 0 0 0) :camera camera})
(def chrono-second {:datetime (t/date-time 2015 1 1 12 0 0) :camera camera})
(def chrono-third {:datetime (t/date-time 2015 1 2 5 0 0) :camera camera})
(def chrono-last {:datetime (t/date-time 2015 1 2 12 0 0) :camera camera})

(facts "album creation"
  (fact "An album is created for a single file's metadata"
    (let [f (clojure.java.io/file "file")
          data {f maginon-metadata}
          result (album (gen-state-helper config) data)]
      (:make (:camera (get (:photos result) f))) => "Maginon"))

  (fact "Can handle invalid metadata"
    (let [f (clojure.java.io/file "file")
          data {f invalid-metadata}
          result (album (gen-state-helper config) data)]
      (keys (get (:photos result) f))) => '(:invalid)))

(facts "metadata extraction"
  (fact "Start date is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state-helper config)
          result (extract-metadata state album)]
      (:datetime-start result) => (:datetime chrono-first)))

  (fact "End date is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state-helper config)
          result (extract-metadata state album)]
      (:datetime-end result) => (:datetime chrono-last)))

  (fact "Make is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state-helper config)
          result (extract-metadata state album)]
      (:make result) => "CamMaker"))

  (fact "Model is extracted"
    (let [album [chrono-second chrono-first chrono-last chrono-third]
          state (gen-state-helper config)
          result (extract-metadata state album)]
      (:model result) => "MyCam")))

(facts "Species Independence"
  (fact "A single sighting is extracted"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}]
          state (gen-state-helper config)]
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 1})))

  (fact "Multiple species are extracted if present"
    (let [sightings [{:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                      :species-scientific-name "Yellow Spotted Housecat"
                      :sighting-quantity 1}
                     {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                      :species-scientific-name "Smiley Wolf"
                      :sighting-quantity 2}]
          state (gen-state-helper config)]
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
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
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
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
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
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
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
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
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
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
      (extract-independent-sightings state sightings) => '({:species "Yellow Spotted Housecat"
                                                            :count 4}
                                                           {:species "Smiley Wolf"
                                                            :count 7}))))