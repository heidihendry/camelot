(ns camelot.import.scan-dir-test
  (:require [camelot.import.scan-dir :as sut]
            [clojure.test :refer :all]
            [camelot.testutil.state :as state]
            [clojure.java.io :as io]
            [camelot.util.file :as file]))

(deftest test-resolve-server-directory
  (with-redefs [file/canonical-path file/get-path]
    (testing "Server directory resolution"
      (testing "Should use client directory if root path not set."
        (let [state (state/gen-state {:paths {:root nil}})]
          (is (= "/srv/mydata/survey1"
                 (sut/resolve-directory state "/srv/mydata/survey1")))))

      (testing "Should know how to treat Windows drive letters coming from the client when root-path not set."
        (let [state (state/gen-state {:paths {:root nil}})]
          (with-redefs [file/path-separator #(constantly "\\")]
            (is (= "G:\\srv\\mydata\\survey1"
                   (sut/resolve-directory state "G:\\srv\\mydata\\survey1"))))))

      (testing "Should use root path if unable to resolve directory."
        (let [state (state/gen-state {:paths {:root (io/file "/my/path")}})]
          (is (= "/my/path"
                 (sut/resolve-directory state "/random/non-matching/location")))))

      (testing "Should know be able to resolve simple directories on nix with nix client."
        (is (= "/srv/research data/camelot/survey1"
               (sut/resolve-server-directory "/srv/research data/camelot"
                                             "/mnt/server/research data/camelot/survey1"))))

      (testing "Should know be able to resolve simple directories on nix with Windows."
        (is (= "/srv/research data/camelot/survey1"
               (sut/resolve-server-directory "/srv/research data/camelot"
                                             "G:\\camelot\\survey1"))))

      (testing "Should know be able to resolve simple directories on Windows with nix client."
        (is (= "G:\\research data\\camelot\\survey1"
               (sut/resolve-server-directory "G:\\research data\\camelot"
                                             "/srv/camelot/survey1"))))

      (testing "Should know be able to resolve simple directories on Windows with Windows client."
        (is (= "G:\\research data\\camelot\\survey1"
               (sut/resolve-server-directory "G:\\research data\\camelot"
                                             "G:\\camelot\\survey1"))))

      (testing "Should handle trailing separators for the server."
        (is (= "/srv/research data/camelot/survey1"
               (sut/resolve-server-directory "/srv/research data/camelot/"
                                             "/srv/camelot/survey1"))))

      (testing "Should handle trailing separators for the client."
        (is (= "/srv/research data/camelot/survey1"
               (sut/resolve-server-directory "/srv/research data/camelot"
                                             "/srv/camelot/survey1/"))))

      (testing "Should resolve relative pathnames."
        (is (= "/srv/research data/camelot/survey1/something"
               (sut/resolve-server-directory "/srv/research data/camelot"
                                             "survey1/something"))))

      (testing "Should resolve relative pathnames from Windows cliet."
        (is (= "/srv/research data/camelot/survey1/something"
               (sut/resolve-server-directory "/srv/research data/camelot"
                                             "survey1\\something")))))))
