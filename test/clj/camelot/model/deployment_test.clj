(ns camelot.model.deployment-test
  (:require [camelot.model.deployment :as sut]
            [clj-time.core :as t]
            [clojure.test :refer [is are deftest testing]]
            [camelot.testutil.mock :refer [defmock with-spies] :as mock]
            [camelot.testutil.state :as state]))

(def base-deployment
  {:trap-station-session-id 10
   :trap-station-session-created (t/date-time 2018 1 1)
   :trap-station-session-updated (t/date-time 2018 3 1)
   :trap-station-id 20
   :trap-station-name "Trap 1"
   :site-id 30
   :survey-site-id 40
   :trap-station-longitude 5.5
   :trap-station-latitude -3
   :site-name "Site 1"
   :trap-station-session-start-date (t/date-time 2018 3 1)
   :trap-station-session-end-date (t/date-time 2018 5 1)
   :camera-id 50
   :camera-name "Camera 1"
   :camera-status-id 2
   :trap-station-session-camera-media-unrecoverable false})

(def make-deployment #(merge base-deployment %))

(def get-all
  [(make-deployment
    {:trap-station-session-id 10
     :trap-station-session-start-date (t/date-time 2018 3 1)
     :trap-station-session-end-date (t/date-time 2018 5 1)})
   (make-deployment
    {:trap-station-session-id 11
     :trap-station-session-start-date (t/date-time 2018 1 1)
     :trap-station-session-end-date (t/date-time 2018 3 1)})
   (make-deployment
    {:trap-station-id 21
     :trap-station-name "Trap 2"
     :trap-station-session-id 12
     :trap-station-session-start-date (t/date-time 2018 3 1)
     :trap-station-session-end-date (t/date-time 2018 4 1)})])

(defn gen-state
  [{:keys [get-all]}]
  (state/gen-state {} {:deployments
                       {:get-all (defmock [p c] get-all)}}))

(defn- first-query
  [calls state ks]
  (let [r (mock/query-params calls state ks)]
    (is (< (count r) 2))
    (first r)))

(defn get-all-params
  [calls state]
  (first-query calls state [:deployments :get-all]))

(deftest test-get-all
  (testing "should pass the expected parameters"
    (with-spies [calls]
      (let [state (gen-state {:get-all get-all})]
        (sut/get-all state 1)
        (is (= (get-all-params (calls) state) {:survey-id 1})))))

  (testing "should get the expected sessions for each deployment"
    (with-spies [calls]
      (let [state (gen-state {:get-all get-all})
            result (sut/get-all state 1)]
        (are [k expected]
            (= (mapv k result) expected)
          :trap-station-session-id [10 12]
          :trap-station-session-end-date [(t/date-time 2018 5 1) (t/date-time 2018 4 1)]))))

  (testing "should return the expected trap stations"
    (with-spies [calls]
      (let [state (gen-state {:get-all get-all})
            result (sut/get-all state 1)]
        (are [k expected]
            (= (mapv k result) expected)
          :trap-station-name ["Trap 1" "Trap 2"]
          :trap-station-id [20 21])))))
