(ns ctdp.album-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [ctdp.album :refer :all]
            [ctdp.config :refer [gen-state]]
            [clj-time.core :as t]
            [schema.test :as st]
            [ctdp.exif-test-metadata :refer :all]))

(use-fixtures :once st/validate-schemas)

(def night (t/date-time 2015 1 1 0 0 0))
(def day (t/date-time 2015 1 1 12 0 0))

(def config
  {:infrared-iso-value-threshold 999
    :night-end-hour 5
    :night-start-hour 21
    :erroneous-infrared-threshold 0.5})

(deftest test-ir-threshold
  (testing "A photo which uses IR at night is okay"
    (let [album [{:datetime night :settings {:iso 1000}}]]
      (is (not (exceed-ir-threshold config album)))))

  (testing "A photo which uses IR in the day is okay"
    (let [album [{:datetime day :settings {:iso 1000}}]]
      (is (not (exceed-ir-threshold config album)))))

  (testing "A photo which does not use IR at night is not okay"
    (let [album [{:datetime night :settings {:iso 999}}]]
      (is (exceed-ir-threshold config album))))

  (testing "One valid and one invalid photo is not okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 999}}]]
      (is (exceed-ir-threshold config album))))

  (testing "Two valid and one invalid photo is okay"
    (let [album [{:datetime night :settings {:iso 999}}
                 {:datetime day :settings {:iso 1000}}
                 {:datetime night :settings {:iso 1000}}]]
      (is (not (exceed-ir-threshold config album))))))

(deftest test-album
  (testing "An album is created for a single file's metadata"
    (let [f (clojure.java.io/file "file")
          data {f maginon-metadata}
          result (album (gen-state config) data)]
      (is (= (:make (:camera (get (:photos result) f))) "Maginon")))))
