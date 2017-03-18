(ns camelot.report.core-test
  (:require [camelot.report.core :as sut]
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
