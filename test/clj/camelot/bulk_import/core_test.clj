(ns camelot.bulk-import.core-test
  (:require [camelot.bulk-import.core :as sut]
            [camelot.test-util.state :as state]
            [clojure.test :refer :all]))

(def test-canonical-path identity)

(deftest test-resolve-server-directory
  (with-redefs [camelot.util.file/canonical-path camelot.util.file/get-path]
    (testing "Server directory resolution"
      (testing "Should use client directory if root path not set."
        (let [state (state/gen-state {:root-path nil})]
          (is (= (sut/resolve-directory state "/srv/mydata/survey1")
                 "/srv/mydata/survey1"))))

      (testing "Should know how to treat Windows drive letters coming from the client when root-path not set."
        (let [state (state/gen-state {:root-path nil})]
          (with-redefs [camelot.util.file/path-separator #(constantly "\\")]
            (is (= (sut/resolve-directory state "G:\\srv\\mydata\\survey1")
                   "G:\\srv\\mydata\\survey1")))))

      (testing "Should use root path if unable to resolve directory."
        (let [state (state/gen-state {:root-path "/my/path"})]
          (is (= (sut/resolve-directory state "/random/non-matching/location")
                 "/my/path"))))

      (testing "Should know be able to resolve simple directories on nix with nix client."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot"
                                             "/mnt/server/research data/camelot/survey1")
               "/srv/research data/camelot/survey1")))

      (testing "Should know be able to resolve simple directories on nix with Windows."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot"
                                             "G:\\camelot\\survey1")
               "/srv/research data/camelot/survey1")))

      (testing "Should know be able to resolve simple directories on Windows with nix client."
        (is (= (sut/resolve-server-directory "G:\\research data\\camelot"
                                             "/srv/camelot/survey1")
               "G:\\research data\\camelot\\survey1")))

      (testing "Should know be able to resolve simple directories on Windows with Windows client."
        (is (= (sut/resolve-server-directory "G:\\research data\\camelot"
                                             "G:\\camelot\\survey1")
               "G:\\research data\\camelot\\survey1")))

      (testing "Should handle trailing separators for the server."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot/"
                                             "/srv/camelot/survey1")
               "/srv/research data/camelot/survey1")))

      (testing "Should handle trailing separators for the client."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot"
                                             "/srv/camelot/survey1/")
               "/srv/research data/camelot/survey1")))

      (testing "Should resolve relative pathnames."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot"
                                             "survey1/something")
               "/srv/research data/camelot/survey1/something")))

      (testing "Should resolve relative pathnames from Windows cliet."
        (is (= (sut/resolve-server-directory "/srv/research data/camelot"
                                             "survey1\\something")
               "/srv/research data/camelot/survey1/something"))))))

(deftest test-default-mapping-assignment
  (testing "Default mapping assignment."
    (testing "should map known default values"
      (let [ps {"Camelot GPS Latitude" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"}))))

    (testing "should be additive."
      (let [ps {"Camelot GPS Latitude" true
                "Camelot GPS Longitude" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"
                :trap-station-longitude "Camelot GPS Longitude"}))))

    (testing "should ignore columns which to not have a default mapping."
      (let [ps {"Camelot GPS Latitude" true
                "Something" true}]
        (is (= (sut/assign-default-mappings ps)
               {:trap-station-latitude "Camelot GPS Latitude"}))))))

(deftest test-file-data-to-record-list
  (testing "transforming file data to records"
    (testing "mapping to null is omitted"
      (with-redefs [camelot.bulk-import.datatype/deserialise (fn [k d] d)]
        (is (= (sut/file-data-to-record-list (state/gen-state)
                                             [["V1-1","V1-2","V1-3"]
                                              ["V2-1","V2-2","V2-3"]]
                                             {"H1" 0 "H2" 1 "H3" 2}
                                             {:h1 "H1" :h2 nil :h3 "H3"})
               [{:h1 "V1-1" :h3 "V1-3"}
                {:h1 "V2-1" :h3 "V2-3"}]))))))
