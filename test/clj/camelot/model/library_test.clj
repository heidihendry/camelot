(ns camelot.model.library-test
  (:require [camelot.model.library :as sut]
            [midje.sweet :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]
            [camelot.model.sighting :as sighting]))

(def media-fixture
  {:media-id 1
   :media-created (t/date-time 2015 1 1 12 59 59)
   :media-updated (t/date-time 2015 1 1 12 59 59)
   :media-filename "myfile.jpg"
   :media-cameracheck false
   :media-attention-needed false
   :media-capture-timestamp (t/date-time 2014 9 8 7 6 5)
   :trap-station-session-camera-id 3
   :trap-station-session-id 53
   :trap-station-id 30
   :trap-station-name "My Trap"
   :trap-station-longitude 105
   :trap-station-latitude 30
   :site-sublocation "Observatory"
   :site-city "Atlantis"
   :camera-id 1
   :camera-name "ABC01"
   :survey-site-id 99
   :survey-id 6
   :site-id 9
   :site-name "My Site"})

(def sighting-fixture
  {:media-id 1
   :sighting-created (t/date-time 2016 1 1 2 3 4)
   :sighting-updated (t/date-time 2016 1 1 2 5 6)
   :sighting-quantity 1
   :sighting-id 2
   :species-id 5})

(defn- mock-record
  [params]
  (merge media-fixture params))

(defn- mock-sighting
  [params]
  (sighting/sighting (merge sighting-fixture params)))

(facts "Library"
  (fact "Constructs media without sighting"
    (let [sightings []
          media [(mock-record {:media-filename "file"})]
          result (sut/build-records (state/gen-state) sightings media)]
      (count result) => 1
      (:sightings (first result)) => []
      (:media-uri (first result)) => "/media/photo/file"))

  (fact "Constructs media, excluding sightings not matching media ID"
    (let [sightings [(mock-sighting {:media-id 30})]
          media [(mock-record {:media-id 1})]
          result (sut/build-records (state/gen-state) sightings media)]
      (count result) => 1
      (:sightings (first result)) => []))

  (fact "Constructs media, including sighting matching media ID"
    (let [sightings [(mock-sighting {:media-id 1})]
          media [(mock-record {:media-id 1})]
          result (sut/build-records (state/gen-state) sightings media)]
      (count result) => 1
      (:sightings (first result)) => sightings)))

  (fact "Constructs media, including multiple sightings matching media ID"
    (let [sightings [(mock-sighting {:media-id 1 :species-id 3})
                     (mock-sighting {:media-id 3 :species-id 3})
                     (mock-sighting {:media-id 1 :species-id 10})]
          media [(mock-record {:media-id 1})]
          result (sut/build-records (state/gen-state) sightings media)]
      (count result) => 1
      (:sightings (first result)) => (filter #(= (:media-id %) 1) sightings)))
