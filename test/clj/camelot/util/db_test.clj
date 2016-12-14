(ns camelot.util.db-test
  (:require
   [camelot.util.db :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [camelot.test-util.state :as state]))

(deftest test-clj-keys
  (testing "Converting from database types"
    (testing "Converts columns in a single record"
      (let [data {:column_name "MyData"}]
        (is (= (sut/clj-keys data) {:column-name "MyData"}))))

    (testing "Converts columns in a list of records"
      (let [data '({:column_name "MyData"}
                   {:column_name "Other Data"})]
        (is (= (sut/clj-keys data) '({:column-name "MyData"}
                                     {:column-name "Other Data"})))))

    (testing "Converts columns in a vector of records"
      (let [data [{:column_name "MyData"}
                  {:column_name "Other Data"}]]
        (is (= (sut/clj-keys data) '({:column-name "MyData"}
                                     {:column-name "Other Data"})))))

    (testing "Returns non-collection data unchanged"
      (let [data "The Number 5"]
        (is (= (sut/clj-keys data) data))))

    (testing "Returns nil when passed nil"
      (let [data nil]
        (is (= (sut/clj-keys data) nil))))

    (testing "Converts from SQL Timestamps"
      (let [date (t/date-time 2015 10 10 1 1 5)
            date-long (tc/to-long date)
            data {:column_name (java.sql.Timestamp. date-long)}]
        (is (= (sut/clj-keys data) {:column-name date}))))))

(deftest test-db-keys
  (testing "Converting to database types"
    (testing "Converts columns in a single record"
      (let [data {:column-name "MyData"}
            state (state/gen-state)]
        (is (= (sut/db-keys data) {:column_name "MyData"}))))

    (testing "Returns an empty hash when passed nil"
      (let [data nil
            state (state/gen-state)]
        (is (= (sut/db-keys data) {}))))

    (testing "Converts to SQL Timestamps"
      (let [date (t/date-time 2015 10 10 1 1 5)
            date-long (tc/to-long date)
            data {:column-name date}
            state (state/gen-state)]
        (is (= (sut/db-keys data) {:column_name (java.sql.Timestamp. date-long)}))))))

(deftest test-with-db-keys
  (testing "Converting between database types"
    (testing "Calls a function with the `db' data, returning a `clj' result."
      (let [fn (fn [a c] (update a :column_name inc))
            data {:column-name 5}
            state (state/gen-state)]
        (is (= (sut/with-db-keys state fn data) {:column-name 6}))))))
