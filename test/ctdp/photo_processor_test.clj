(ns ctdp.photo-processor-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [ctdp.photo-processor :refer :all]
            [schema.test :as st]
            [ctdp.exif-test-metadata :refer :all]))

(use-fixtures :once st/validate-schemas)

(deftest metadata-normalisation
  (testing "Maginon metadata normalises okay"
    (let [output (vendor maginon-metadata)]
      (is (= (:filesize output) 1175819))
      (is (= (:make (:camera output)) :maginon))))

  (testing "Cuddeback metadata normalises okay"
    (let [output (vendor cuddeback-metadata)]
      (is (= (:filesize output) 513653))
      (is (= (:make (:camera output)) :cuddeback)))))
