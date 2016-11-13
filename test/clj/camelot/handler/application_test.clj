(ns camelot.handler.application-test
  (:require
   [camelot.handler.application :refer :all]
   [clojure.test :refer :all]
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest test-metadata-paths
  (testing "Metadata flattening"
    (testing "Datastructure produced is vector of paths"
      (let [data metadata-paths]
        (is (= (every? identity (flatten (map #(map keyword? %) data))) true))
        (is (= (every? identity (map vector? data)) true))
        (is (= (vector? data) true))))

    (testing "An entry is available for GPS Location"
      (let [search [:location :gps-longitude]]
        (is (= (some #{search} metadata-paths) search))))))

