(ns camelot.bulk-import.core-test
  (:require [camelot.bulk-import.core :as sut]
            [camelot.test-util.state :as state]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(deftest test-resolve-server-directory
  (testing "Server directory resolutino"
    (testing "Should use client directory if root path not set."
      (let [state (state/gen-state {:root-path nil})]
        (is (= (sut/resolve-directory state "/srv/mydata/survey1")
               (io/file "/srv/mydata/survey1")))))

    (testing "Should know how to treat Windows drive letters coming from the client when root-path not set."
      (let [state (state/gen-state {:root-path nil})]
        (with-redefs [camelot.app.state/get-os #(constantly :windows)]
          (is (= (sut/resolve-directory state "G:\\srv\\mydata\\survey1")
                 (io/file "G:\\srv\\mydata\\survey1"))))))

    (testing "Should use root path if unable to resolve directory."
      (let [state (state/gen-state {:root-path "/my/path"})]
        (is (= (sut/resolve-directory state "/random/non-matching/location")
               (io/file "/my/path")))))

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
             "/srv/research data/camelot/survey1")))))
