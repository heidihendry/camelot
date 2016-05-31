(ns camelot.db-test
  (:require [camelot.db :as sut]
            [midje.sweet :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))

(facts "Converting from database types"
  (fact "Converts columns in a single record"
    (let [data {:column_name "MyData"}]
      (sut/clj-keys data) => {:column-name "MyData"}))

  (fact "Converts columns in a list of records"
    (let [data '({:column_name "MyData"}
                 {:column_name "Other Data"})]
      (sut/clj-keys data) => '({:column-name "MyData"}
                               {:column-name "Other Data"})))

  (fact "Converts columns in a vector of records"
    (let [data [{:column_name "MyData"}
                {:column_name "Other Data"}]]
      (sut/clj-keys data) => '({:column-name "MyData"}
                               {:column-name "Other Data"})))

  (fact "Returns non-collection data unchanged"
    (let [data "The Number 5"]
      (sut/clj-keys data) => data))

  (fact "Returns nil when passed nil"
    (let [data nil]
      (sut/clj-keys data) => nil))

  (fact "Converts from SQL Timestamps"
    (let [date (t/date-time 2015 10 10 1 1 5)
          date-long (tc/to-long date)
          data {:column_name (java.sql.Timestamp. date-long)}]
      (sut/clj-keys data) => {:column-name date})))

(facts "Converting to database types"
  (fact "Converts columns in a single record"
    (let [data {:column-name "MyData"}
          state {}]
      (sut/db-keys data) => {:column_name "MyData"}))

  (fact "Returns an empty hash when passed nil"
    (let [data nil
          state {}]
      (sut/db-keys data) => {}))

  (fact "Converts to SQL Timestamps"
    (let [date (t/date-time 2015 10 10 1 1 5)
          date-long (tc/to-long date)
          data {:column-name date}
          state {}]
      (sut/db-keys data) => {:column_name (java.sql.Timestamp. date-long)})))

(facts "Converting between database types"
  (fact "Calls a function with the `db' data, returning a `clj' result."
    (let [fn #(update % :column_name inc)
          data {:column-name 5}
          state {}]
      (sut/with-db-keys state fn data) => {:column-name 6}))

  (fact "Calls function with a connection should one be provided."
    (let [fn (fn
               ([data opts] (:connection opts))
               ([data] false))
          data {:column-name 5}
          state {:connection true}]
      (sut/with-db-keys state fn data) => true)))
