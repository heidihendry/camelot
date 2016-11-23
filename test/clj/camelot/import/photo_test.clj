(ns camelot.import.photo-test
  (:require
   [camelot.fixtures.exif-test-metadata :refer :all]
   [camelot.import.photo :as photo]
   [camelot.test-util.state :as state]
   [clj-time.core :as t]
   [clojure.data :refer [diff]]
   [clojure.test :refer :all]
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest test-metadata-parsing
  (testing "Metadata parsing"
    (testing "Maginon metadata normalises okay"
      (let [output (photo/parse (state/gen-state {}) maginon-metadata)]
        (is (= (:filesize output) 1175819))
        (is (= (:make (:camera output)) "Maginon"))
        (is (= (:datetime output) (t/date-time 2014 4 11 16 37 52)))))

    (testing "Cuddeback metadata normalises okay"
      (let [output (photo/parse (state/gen-state {}) cuddeback-metadata)]
        (is (= (:filesize output) 513653))
        (is (= (:make (:camera output)) "CUDDEBACK"))
        (is (= (:datetime output) (t/date-time 2014 4 11 19 47 46)))))

    (testing "Metadata with nil required fields is not valid"
      (let [config {:language :en}
            res (photo/parse (state/gen-state config) {})]
        (is (= (contains? res :invalid) true))
        (is (= (boolean (re-find #"Date/Time" (:invalid res))) true))))))

(deftest test-get-time-difference
  (testing "Timeshift"
    (testing "Small positive timeshift is correct"
      (let [dt-a (t/date-time 2015 3 14 13 1 26)
            dt-b (t/date-time 2015 3 14 13 3 26)]
        (is (= (photo/get-time-difference dt-a dt-b) 120))))

    (testing "Small negative timeshift is correct"
      (let [dt-a (t/date-time 2015 3 14 13 5 26)
            dt-b (t/date-time 2015 3 14 13 2 10)]
        (is (= (photo/get-time-difference dt-a dt-b) -196))))

    (testing "Large timeshift is correct"
      (let [dt-a (t/date-time 2015 3 14 13 5 26)
            dt-b (t/date-time 2001 3 14 13 5 26)]
        (is (= (photo/get-time-difference dt-a dt-b) -441763200))))))
