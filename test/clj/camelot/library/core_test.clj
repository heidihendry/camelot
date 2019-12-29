(ns camelot.library.core-test
  (:require
   [camelot.library.core :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [camelot.testutil.state :as state]
   [camelot.model.sighting :as sighting]
   [camelot.model.suggestion :as suggestion]))

(def media-fixture
  {:media-id 1
   :media-created (t/date-time 2015 1 1 12 59 59)
   :media-updated (t/date-time 2015 1 1 12 59 59)
   :media-filename "myfile.jpg"
   :media-format "jpg"
   :media-cameracheck false
   :media-attention-needed false
   :media-processed false
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
   :survey-name "My Survey"
   :site-id 9
   :site-name "My Site"})

(def sighting-fixture
  {:media-id 1
   :sighting-created (t/date-time 2016 1 1 2 3 4)
   :sighting-updated (t/date-time 2016 1 1 2 5 6)
   :sighting-quantity 1
   :sighting-id 2
   :species-id 5})

(def suggestion-fixture
  {:suggestion-id 1
   :media-id 1
   :suggestion-created (t/date-time 2016 1 1 2 3 4)
   :suggestion-updated (t/date-time 2016 1 1 2 5 6)
   :suggestion-key "123"
   :suggestion-label "Human"
   :suggestion-confidence 0.8
   :bounding-box nil})

(defn- mock-record
  [params]
  (merge media-fixture params))

(defn- mock-sighting
  [params]
  (sighting/sighting (merge sighting-fixture params)))

(defn- mock-suggestion
  [params]
  (suggestion/suggestion (merge suggestion-fixture params)))

(deftest test-build-records
  (testing "Library"
    (testing "Constructs media without sighting"
      (let [sightings []
            suggestions []
            media [(mock-record {:media-filename "file"})]
            result (sut/build-records (state/gen-state) sightings suggestions media)]
        (is (= (count result) 1))
        (is (= (:sightings (first result)) []))
        (is (= (:media-uri (first result)) "/media/photo/file"))))

    (testing "Constructs media, excluding sightings not matching media ID"
      (let [sightings [(mock-sighting {:media-id 30})]
            suggestions []
            media [(mock-record {:media-id 1})]
            result (sut/build-records (state/gen-state) sightings suggestions media)]
        (is (= (count result) 1))
        (is (= (:sightings (first result)) []))))

    (testing "Constructs media, including sighting matching media ID"
      (let [sightings [(mock-sighting {:media-id 1})]
            suggestions []
            media [(mock-record {:media-id 1})]
            result (sut/build-records (state/gen-state) sightings suggestions media)]
        (is (= (count result) 1))
        (is (= (:sightings (first result)) sightings))))

    (testing "Constructs media, including multiple sightings matching media ID"
      (let [sightings [(mock-sighting {:media-id 1 :species-id 3})
                       (mock-sighting {:media-id 3 :species-id 4})
                       (mock-sighting {:media-id 1 :species-id 10})]
            suggestions []
            media [(mock-record {:media-id 1})]
            result (sut/build-records (state/gen-state) sightings suggestions media)]
        (is (= (count result) 1))
        (is (= (:sightings (first result)) (filter #(= (:media-id %) 1) sightings)))))

    (testing "Constructs media, including multiple suggestions matching media ID"
      (let [sightings []
            suggestions [(mock-suggestion {:media-id 1 :suggestion-id 3})
                         (mock-suggestion {:media-id 3 :suggestion-id 4})
                         (mock-suggestion {:media-id 1 :suggestion-id 10})]
            media [(mock-record {:media-id 1})]
            result (sut/build-records (state/gen-state) sightings suggestions media)]
        (is (= 1 (count result)))
        (is (= (filter #(= (:media-id %) 1) suggestions)
               (:suggestions (first result))))))))

(deftest test-identify
  (testing "Identifying media"
    (testing "should reject media across multiple surveys"
      (with-redefs [camelot.model.media/get-with-ids
                    (constantly [{:media-id 1 :survey-id 1}
                                 {:media-id 2 :survey-id 2}])]
        (is (thrown? IllegalArgumentException (sut/identify (state/gen-state)
                                                            {:identification {}
                                                             :media [1 2]})))))

    (testing "should accept media within a single survey"
      (with-redefs [camelot.model.media/get-with-ids
                    (constantly [{:media-id 1 :survey-id 1}
                                 {:media-id 2 :survey-id 1}])
                    camelot.model.media/update-processed-flag! (constantly nil)
                    camelot.model.sighting/create!
                    (fn [s v] {:sighting-id (inc (:media-id v))})
                    clojure.java.jdbc/db-transaction* (fn [s q] q)]
        (is (sut/identify (state/gen-state)
                          {:identification {}
                           :media [1 2]}))))))
