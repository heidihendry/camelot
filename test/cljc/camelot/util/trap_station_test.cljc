(ns camelot.util.trap-station-test
  (:require
   [camelot.util.trap-station :as sut]
   #?(:clj
      [clojure.test :refer [deftest is testing use-fixtures]]
      :cljs
      [cljs.test :refer-macros [deftest is testing use-fixtures]])
   [schema.test :as st]))

#?(:clj (use-fixtures :once st/validate-schemas))

(deftest test-latitude
  (testing "Latitude"
    (testing "Zero latitude should return true"
      (is (= (sut/valid-latitude? 0) true)))

    (testing "Misc decimal latitude should return true"
      (is (= (sut/valid-latitude? 33.34) true)))

    (testing "Minimum latitude should return true"
      (is (= (sut/valid-latitude? -90.0) true)))

    (testing "Maximum latitude should return true"
      (is (= (sut/valid-latitude? 90.0) true)))

    (testing "Beyond minimum latitude should return false"
      (is (= (sut/valid-latitude? -90.1) false)))

    (testing "Beyond maximum latitude should return false"
      (is (= (sut/valid-latitude? 90.1) false)))))

(deftest test-longitude
  (testing "Longitude"
    (testing "of zero should return true"
      (is (= (sut/valid-longitude? 0) true)))

    (testing "with valid decimal value should return true"
      (is (= (sut/valid-longitude? 33.34) true)))

    (testing "minimum valid value should return true"
      (is (= (sut/valid-longitude? -180.0) true)))

    (testing "maximum valid value should return true"
      (is (= (sut/valid-longitude? 180.0) true)))

    (testing "beyond minimum should return false"
      (is (= (sut/valid-longitude? -180.1) false)))

    (testing "beyond maximum longitude should return false"
      (is (= (sut/valid-longitude? 180.1) false)))))

