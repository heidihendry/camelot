(ns camelot.model.sighting-field-value-test
  (:require [camelot.model.sighting-field-value :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [camelot.testutil.state :as state]
            [camelot.testutil.mock :refer [defmock with-spies] :as mock]))

(def ^:dynamic *get-for-sighting*
  [{:sighting-field-value-id 2
    :sighting-field-value-created (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-updated (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-data "Value"
    :sighting-field-id 5
    :sighting-id 6}])

(def ^:dynamic *create<!* {:1 1})

(def ^:dynamic *get-specific*
  [{:sighting-field-value-id 1
    :sighting-field-value-created (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-updated (t/date-time 2017 1 1 20 30 40)
    :sighting-field-value-data "Value"
    :sighting-field-id 2
    :sighting-id 3}])

(def update-for-sighting! sut/update-for-sighting!)

(defn gen-state
  []
  (state/gen-state {} {:sighting-field-value
                       {:get-for-sighting (defmock [p c] *get-for-sighting*)
                        :create<! (defmock [p c] *create<!*)
                        :get-specific (defmock [p c] *get-specific*)
                        :update! (defmock [p c] nil)}}))

(defn- first-query
  [calls state ks]
  (let [r (mock/query-params calls state ks)]
    (is (< (count r) 2))
    (first r)))

(defn create-params
  [calls state]
  (first-query calls state [:sighting-field-value :create<!]))

(defn update-params
  [calls state]
  (first-query calls state [:sighting-field-value :update!]))

(deftest test-update-for-sighting
  (testing "update-for-sighting"
    (testing "should create new record if existing value not found"
      (with-spies [calls]
        (let [state (gen-state)]
          (update-for-sighting! state 3 {1 "value"})
          (is (= (create-params (calls) state)
                 {:sighting-field-id 1
                  :sighting-field-value-data "value"
                  :sighting-id 3})))))

    (testing "should update record if existing value is found"
      (with-spies [calls]
        (let [state (gen-state)]
          (update-for-sighting! state 6 {5 "Updated value"})
          (is (= (update-params (calls) state)
                 {:sighting-field-value-id 2
                  :sighting-field-value-data "Updated value"})))))

    (testing "should mix updates and creates"
      (with-spies [calls]
        (let [state (gen-state)]
          (update-for-sighting! state 6 {5 "Updated value"
                                         1 "Create value"})
          (is (= (update-params (calls) state)
                 {:sighting-field-value-id 2
                  :sighting-field-value-data "Updated value"}))
          (is (= (create-params (calls) state)
                 {:sighting-field-id 1
                  :sighting-field-value-data "Create value"
                  :sighting-id 6})))))

    (testing "should not create or update a field if no change needed"
      (with-spies [calls]
        (let [state (gen-state)]
          (update-for-sighting! state 6 {5 "Value"})
          (is (= (update-params (calls) state) nil))
          (is (= (create-params (calls) state) nil)))))

    (testing "should not create or update a field if no change needed"
      (with-spies [calls]
        (let [state (gen-state)]
          (update-for-sighting! state 6 {5 "Value"})
          (is (= (update-params (calls) state) nil))
          (is (= (create-params (calls) state) nil)))))))
