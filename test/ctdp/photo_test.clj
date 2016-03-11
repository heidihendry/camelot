(ns ctdp.photo-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [ctdp.photo :refer :all]
            [schema.test :as st]
            [clj-time.core :as t]
            [ctdp.exif-test-metadata :refer :all]))

(use-fixtures :once st/validate-schemas)

(deftest test-normalisation
  (testing "Maginon metadata normalises okay"
    (let [output (normalise maginon-metadata)]
      (is (= (:filesize output) 1175819))
      (is (= (:make (:camera output)) "Maginon"))
      (is (= (:datetime output) (t/date-time 2014 4 11 16 37 52)))))

  (testing "Cuddeback metadata normalises okay"
    (let [output (normalise cuddeback-metadata)]
      (is (= (:filesize output) 513653))
      (is (= (:make (:camera output)) "CUDDEBACK"))
      (is (= (:datetime output) (t/date-time 2014 4 11 19 47 46))))))
