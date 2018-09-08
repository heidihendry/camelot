(ns camelot.model.camera-deployment-test
  (:require [camelot.model.camera-deployment :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]))

(def default-record
  {:trap-station-session-id 1
   :trap-station-name "Name"
   :site-id 2
   :trap-station-id 3
   :trap-station-longitude 10
   :trap-station-latitude 20
   :trap-station-session-start-date (t/date-time 2016 1 1)
   :trap-station-session-end-date (t/date-time 2016 1 5)
   :primary-camera-id 1
   :primary-camera-name "CAM1"
   :primary-camera-original-id 1
   :primary-camera-status-id 1
   :primary-camera-media-unrecoverable false
   :secondary-camera-id 2
   :secondary-camera-name "CAM2"
   :secondary-camera-original-id 1
   :secondary-camera-status-id 1
   :secondary-camera-media-unrecoverable false})

(defn make-record
  [data]
  (sut/tcamera-deployment (merge default-record data)))

(defn create!
  [data]
  (sut/create-camera-check! (state/gen-state) (make-record data)))

(deftest test-valid-camera-check?
  (testing "valid-camera-check?"
    (testing "Should return false if primary and secondary cameras are the same."
      (let [data {:primary-camera-id 1 :secondary-camera-id 1}]
        (is (= (sut/valid-camera-check? data) false))))

    (testing "Should return false if end date is before start date."
      (let [data {:primary-camera-id 1
                  :secondary-camera-id 2
                  :trap-station-session-start-date (t/date-time 2016 1 5)
                  :trap-station-session-end-date (t/date-time 2016 1 1)}]
        (is (= (sut/valid-camera-check? data) false))))

    (testing "Should return true otherwise."
      (let [data {:primary-camera-id 1
                  :secondary-camera-id 2
                  :trap-station-session-start-date (t/date-time 2016 1 1)
                  :trap-station-session-end-date (t/date-time 2016 1 5)}]
        (is (= (sut/valid-camera-check? data) true)))))

  (testing "create-camera-check!"
    (testing "Invalid camera deployment fails precondition check."
      (is (thrown? java.lang.AssertionError
                   (create! {:primary-camera-id 1
                             :secondary-camera-id 1}))))))
