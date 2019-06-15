(ns camelot.report.query-test
  (:require [camelot.report.query :as sut]
            [clojure.test :refer :all]))

(deftest resolution-order-test
  (testing "Resolution order"
    (testing "Correct order for :media-id"
      (let [r (sut/resolution-order sut/data-definitions :media-id)]
        (is (= r [[:media-id :media-id]
                  [:trap-station-session-camera-id :trap-station-session-camera-id]
                  [:trap-station-session-id :trap-station-session-id]
                  [:camera-id :camera-id]
                  [:trap-station-id :trap-station-id]
                  [:camera-status-id :camera-status-id]
                  [:survey-site-id :survey-site-id]
                  [:survey-id :survey-id]
                  [:site-id :site-id]
                  [:sighting-id :media-id]
                  [:taxonomy-id :taxonomy-id]
                  [:species-mass-id :species-mass-id]
                  [:photo-id :media-id]]))))))

(deftest build-records
  (testing "Build records"
    (testing "Should resolve from initial path with single field"
      (let [rorder [[:field-a :field-a]]
            data {:field-a {:field-a {1 [{:field-a 1}]}}}]
        (is (= (sut/build-records rorder data) [{:field-a 1}]))))

    (testing "Should resolve from initial path with field relationship"
      (let [rorder [[:field-a :field-b]]
            data {:field-a {:field-b {1 [{:field-a 2 :field-b 1}]}}}]
        (is (= (sut/build-records rorder data) [{:field-a 2 :field-b 1}]))))

    (testing "Should resolve a second field using data from the initial record"
      (let [rorder [[:field-a :field-a]
                    [:field-b :field-a]]
            data {:field-a {:field-a {1 [{:field-a 1}]}}
                  :field-b {:field-a {1 [{:field-a 1 :field-b 2}]}}}]
        (is (= (sut/build-records rorder data) [{:field-a 1 :field-b 2}]))))

    (testing "Should treat initial record as a basis for multiple records if available"
      (let [rorder [[:field-a :field-a]
                    [:field-b :field-a]]
            data {:field-a {:field-a {1 [{:field-a 1}]}}
                  :field-b {:field-a {1 [{:field-a 1 :field-b 2}
                                         {:field-a 1 :field-b 3}]}}}]
        (is (= (sut/build-records rorder data) [{:field-a 1 :field-b 2}
                                                {:field-a 1 :field-b 3}]))))

    (testing "Should treat initial record as a basis for multiple records if available"
      (let [rorder [[:field-a :field-a]
                    [:field-b :field-a]]
            data {:field-a {:field-a {1 [{:field-a 1}]}}
                  :field-b {:field-a {1 [{:field-a 1 :field-b 2}
                                         {:field-a 1 :field-b 3}]}}}]
        (is (= (sut/build-records rorder data) [{:field-a 1 :field-b 2}
                                                {:field-a 1 :field-b 3}]))))))
