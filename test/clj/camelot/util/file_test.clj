(ns camelot.util.file-test
  (:require
   [camelot.util.file :as sut]
   [clojure.test :refer :all]
   [clojure.java.io :as io]
   [camelot.test-util.state :as state]))

(defn state
  [p]
  (state/gen-state {:root-path p}))

(deftest test-file-utils
  (testing "File utils"
    (testing "Should return components relative to the root dir"
      (is (= (sut/rel-path-components (state "/path/to")
                                      (io/file "/path/to/subdir/file"))
             ["subdir" "file"])))

    (testing "Should not care if path ends in separator"
      (is (= (sut/rel-path-components (state "/path/to/")
                                      (io/file "/path/to/subdir/file"))
             ["subdir" "file"])))))
