(ns camelot.util.deployment-test
  (:require
   [camelot.util.deployment :as sut]
   #?(:clj
      [clojure.test :refer [deftest is testing use-fixtures]]
      :cljs
      [cljs.test :refer-macros [deftest is testing use-fixtures]])
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest test-primary-secondary-cameras-as-list
  (testing "Camera input standardisation"
    (testing "Should convert primary and secondary camera data to a list of cameras"
      (let [data {:primary-camera-id 1
                  :primary-camera-status-id 2
                  :secondary-camera-id 2
                  :other 1}]
        (is (= (sut/map-with-cameras-as-list data)
               {:other 1
                :cameras [{:camera-id 1
                           :camera-status-id 2}
                          {:camera-id 2}]}))))))

(def default-session-data
  {:trap-station-session-id 9
   :camera-id 1
   :camera-name "CAM"
   :camera-status-id 2
   :camera-media-unrecoverable false})

(defn ->camera
  [c]
  (merge default-session-data c))

(deftest test-assoc-cameras
  (testing "Add primary/secondary camera information"
    (testing "Should add details for a session"
      (is (= (sut/assoc-cameras [(->camera {})])
             [{:trap-station-session-id 9
               :primary-camera-id 1
               :primary-camera-name "CAM"
               :primary-camera-status-id 2
               :primary-camera-media-unrecoverable false}])))

    (testing "Should add secondary details for given two sessions"
      (is (= (sut/assoc-cameras [(->camera {})
                                 (->camera {:camera-id 2
                                            :camera-name "CAM2"})])
             [{:trap-station-session-id 9
               :primary-camera-id 1
               :primary-camera-name "CAM"
               :primary-camera-status-id 2
               :primary-camera-media-unrecoverable false
               :secondary-camera-id 2
               :secondary-camera-name "CAM2"
               :secondary-camera-status-id 2
               :secondary-camera-media-unrecoverable false}])))))

(deftest test-original-camera-removed?
  (testing "Original camera removed predicate"
    (testing "should consider the original camera removed when its status is not the active status"
      (is (= (sut/original-camera-removed? 2 {:camera-status-id 1
                                              :camera-id 1
                                              :camera-original-id 1})
             true)))

    (testing "should not consider the original camera removed when its status is the active status"
      (is (= (sut/original-camera-removed? 2 {:camera-status-id 2
                                              :camera-id 1
                                              :camera-original-id 1})
             false)))

    (testing "should consider the original camera removed when another camera is introduced"
      (is (= (sut/original-camera-removed? 2 {:camera-status-id 2
                                              :camera-id 2
                                              :camera-original-id 1})
             true)))

    (testing "should not consider the original camera removed if it never existed"
      (is (= (sut/original-camera-removed? 2 {:camera-status-id 2
                                              :camera-id 2
                                              :camera-original-id nil})
             false))
      (is (= (sut/original-camera-removed? 2 {:camera-status-id 2
                                              :camera-id 2
                                              :camera-original-id -1})
             false)))))

(deftest test-camera-active?
  (testing "Camera active predicate"
    (testing "should consider the camera active when no original camera is present."
      (is (= (sut/camera-active? 2 {:camera-id 1
                                    :camera-status-id nil
                                    :camera-original-id nil})
             true)))

    (testing "should consider the camera active its status ID is the active ID."
      (is (= (sut/camera-active? 2 {:camera-id 1
                                    :camera-status-id 2
                                    :camera-original-id 1})
             true)))

    (testing "should consider the camera active when its ID differs to the original ID."
      (is (= (sut/camera-active? 2 {:camera-id 1
                                    :camera-status-id 1
                                    :camera-original-id 2})
             true)))

    (testing "should not consider the active when its ID is the same as the original ID and a non-active status is used."
      (is (= (sut/camera-active? 2 {:camera-id 1
                                    :camera-status-id 1
                                    :camera-original-id 1})
             false)))

    (testing "should not consider the camera active when it's ID is -1 or nil."
      (is (= (sut/camera-active? 2 {:camera-id -1
                                    :camera-status-id 2
                                    :camera-original-id nil}) false))
      (is (= (sut/camera-active? 2 {:camera-id nil
                                    :camera-status-id 2
                                    :camera-original-id nil}) false)))))
