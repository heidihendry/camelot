(ns camelot.import.album-test
  (:require
   [camelot.fixtures.exif-test-metadata :refer :all]
   [camelot.import.album :refer :all]
   [clj-time.core :as t]
   [clojure.data :refer [diff]]
   [clojure.test :refer :all]
   [schema.test :as st]
   [camelot.test-util.state :as state]))

(defn gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(use-fixtures :once st/validate-schemas)

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

(deftest test-album-creation
  (testing "album creation"
    (testing "An album is created for a single file's metadata"
      (let [f (clojure.java.io/file "file")
            data {f maginon-metadata}
            result (album (gen-state-helper config) data)]
        (is (= (:make (:camera (get (:photos result) f))) "Maginon"))))

    (testing "Can handle invalid metadata"
      (is (= (let [f (clojure.java.io/file "file")
                   data {f invalid-metadata}
                   result (album (gen-state-helper config) data)]
               (keys (get (:photos result) f))) '(:invalid))))))

(deftest test-metadata-extraction
  (testing "metadata extraction"
    (testing "Start date is extracted"
      (let [album [chrono-second chrono-first chrono-last chrono-third]
            state (gen-state-helper config)
            result (extract-metadata state album)]
        (is (= (:datetime-start result) (:datetime chrono-first)))))

    (testing "End date is extracted"
      (let [album [chrono-second chrono-first chrono-last chrono-third]
            state (gen-state-helper config)
            result (extract-metadata state album)]
        (is (= (:datetime-end result) (:datetime chrono-last)))))

    (testing "Make is extracted"
      (let [album [chrono-second chrono-first chrono-last chrono-third]
            state (gen-state-helper config)
            result (extract-metadata state album)]
        (is (= (:make result) "CamMaker"))))

    (testing "Model is extracted"
      (let [album [chrono-second chrono-first chrono-last chrono-third]
            state (gen-state-helper config)
            result (extract-metadata state album)]
        (is (= (:model result) "MyCam"))))))
