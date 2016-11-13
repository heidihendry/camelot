(ns camelot.application-test
  (:require
   [camelot.test-util.state :as state]
   [camelot.application :as sut]
   [clojure.test :refer :all]))

(defn gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(def defined-screens
  #{:camera :survey :survey-site :site :trap-station :trap-station-session
    :trap-station-session-camera :taxonomy :photo :media :sighting :settings})

(deftest test-screen-smith
  (testing "Screen smith"
    (testing "Should contain keys for all known screens"
      (is (= (set (keys (sut/all-screens (gen-state-helper {})))) defined-screens)))

    (testing "Schemas should have labels"
      (let [path [:survey :schema :survey-name :label]]
        (is (= (get-in (sut/all-screens (gen-state-helper {})) path) "Survey name"))))

    (testing "Schemas should have descriptions"
      (let [path [:site :schema :site-name :description]]
        (is (= (type (get-in (sut/all-screens (gen-state-helper {})) path)) String))))

    (testing "Schemas should have field schema types"
      (let [path [:site :schema :site-sublocation :schema :type]]
        (is (= (get-in (sut/all-screens (gen-state-helper {})) path) :text))))

    (testing "Camera schema should have a `:camera' resource type"
      (let [path [:camera :resource :type]]
        (is (= (get-in (sut/all-screens (gen-state-helper {})) path) :camera))))

    (testing "Resource title should be translated'"
      (let [path [:trap-station-session-camera :resource :title]]
        (is (= (get-in (sut/all-screens (gen-state-helper {})) path) "Session Camera"))))))
