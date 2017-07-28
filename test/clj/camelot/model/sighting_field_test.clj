(ns camelot.model.sighting-field-test
  (:require
   [camelot.model.sighting-field :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [camelot.testutil.state :as state]
   [camelot.testutil.mock :refer [defmock with-spies] :as mock]
   [clojure.java.jdbc :as jdbc]))

(def ^:dynamic *get-specific*
  [{:sighting-field-id 3
    :sighting-field-created (t/date-time 2017 1 1 10 15),
    :sighting-field-updated (t/date-time 2017 1 2 10 15)
    :sighting-field-key "store-value",
    :sighting-field-label "tore value",
    :sighting-field-datatype :text,
    :sighting-field-default "",
    :sighting-field-required false,
    :sighting-field-affects-independence true,
    :sighting-field-ordering 10,
    :survey-id 1}])

(def ^:dynamic *get-options*
  [{:sighting-field-option-id 10
    :sighting-field-option-label "Option 1"}])

(defn gen-state
  []
  (state/gen-state {} {:sighting-field
                       {:update! (defmock [p c] nil)
                        :delete-options! (defmock [p c] nil)
                        :create-option<! (defmock [p c] nil)
                        :get-specific (defmock [p c] *get-specific*)
                        :get-options (defmock [p c] *get-options*)}}))

(defn- first-query
  [args state ks]
  (let [r (mock/query-params args state ks)]
    (is (< (count r) 2))
    (first r)))

(defn update!-params
  [args state]
  (first-query args state [:sighting-field :update!]))

(defn delete-options!-params
  [args state]
  (first-query args state [:sighting-field :delete-options!]))

(defn update!
  [state id params]
  (with-redefs [jdbc/db-transaction* (fn [db body & ks] (body {}))]
    (sut/update! state id params)))

(deftest test-update!
  (testing "update!"
    (testing "should update with given field config"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :select
                            :sighting-field-key "individual"})
          (is (= (update!-params (args) state)
                 {:sighting-field-datatype "select"
                  :sighting-field-key "individual"
                  :sighting-field-id 3})))))

    (testing "should delete options associated with field"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :select
                            :sighting-field-key "individual"})
          (is (= (delete-options!-params (args) state)
                 {:sighting-field-id 3})))))

    (testing "should create new options for field types with options"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :select
                            :sighting-field-key "individual"
                            :sighting-field-options
                            ["Option 1" "Option 2"]})
          (is (= (mock/query-params (args) state [:sighting-field :create-option<!])
                 [{:sighting-field-id 3
                   :sighting-field-option-label "Option 1"}
                  {:sighting-field-id 3
                   :sighting-field-option-label "Option 2"}])))))

    (testing "should delete options before creating new options"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :select
                            :sighting-field-key "individual"
                            :sighting-field-options
                            ["Option 1" "Option 2"]})
          (mock/query-order-is args state
                               [:sighting-field :delete-options!]
                               [:sighting-field :create-option<!]
                               [:sighting-field :create-option<!]))))

    (testing "should not create options if the datatype does not support it"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :text
                            :sighting-field-key "individual"
                            :sighting-field-options
                            ["Option 1" "Option 2"]})
          (is (empty? (mock/query-params (args) state [:sighting-field :create-option<!]))))))

    (testing "should delete options even if the datatype (now) not support them"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :text
                            :sighting-field-key "individual"})
          (is (seq (mock/query-params (args) state [:sighting-field :delete-options!]))))))

    (testing "should update before creating new options"
      (with-spies [args]
        (let [state (gen-state)]
          (update! state 3 {:sighting-field-datatype :select
                            :sighting-field-key "individual"
                            :sighting-field-options
                            ["Option 1" "Option 2"]})
          (mock/query-order-is args state
                               [:sighting-field :update!]
                               [:sighting-field :create-option<!]
                               [:sighting-field :create-option<!]))))

    (testing "should return updated result"
      (with-spies [args]
        (let [state (gen-state)
              result (update! state 3 {:sighting-field-datatype :select
                                        :sighting-field-key "individual"
                                        :sighting-field-options
                                       ["Option 1"]})]
          (is (= (into {} result)
                 (assoc (first *get-specific*)
                        :sighting-field-options '("Option 1")))))))))
