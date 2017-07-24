(ns camelot.model.sighting-field-value-test
  (:require [camelot.model.sighting-field-value :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]
            [camelot.test-util.mock-db :refer [with-mocked-db with-args defmock with-mocking]]))

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

(def update-for-sighting!
  (with-mocked-db sut/update-for-sighting!))

(use-fixtures :each (fn [f]
                      (with-mocking
                        (defmock -get-for-sighting *get-for-sighting*)
                        (defmock -create<! *create<!*)
                        (defmock -get-specific *get-specific*)
                        (defmock -update! nil)
                        (f))))

(deftest test-update-for-sighting
  (testing "update-for-sighting"
    (testing "should create new record if existing value not found"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 3 {1 "value"})
        (is (= (get args '-create<!)
               [{:sighting-field-id 1
                 :sighting-field-value-data "value"
                 :sighting-id 3}]))))

    (testing "should update record if existing value is found"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Updated value"})
        (is (= (get args '-update!)
               [{:sighting-field-value-id 2
                 :sighting-field-value-data "Updated value"}]))))

    (testing "should mix updates and creates"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Updated value"
                                                   1 "Create value"})
        (is (= (get args '-update!)
               [{:sighting-field-value-id 2
                 :sighting-field-value-data "Updated value"}]))
        (is (= (get args '-create<!)
               [{:sighting-field-id 1
                 :sighting-field-value-data "Create value"
                 :sighting-id 6}]))))

    (testing "should not create or update a field if no change needed"
      (with-args [args]
        (update-for-sighting! (state/gen-state) 6 {5 "Value"})
        (is (= (get args '-update!) nil))
        (is (= (get args '-create<!) nil))))))
