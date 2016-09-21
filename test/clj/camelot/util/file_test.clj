(ns camelot.util.file-test
  (:require [camelot.util.file :as sut]
            [midje.sweet :refer :all]
            [clojure.java.io :as io]))

(defn state
  [p]
  {:config {:root-path p}})

(facts "File utils"
  (fact "Should return components relative to the root dir"
    (sut/rel-path-components (state "/path/to")
                             (io/file "/path/to/subdir/file")) =>
    ["subdir" "file"])

  (fact "Should not care if path ends in separator"
    (sut/rel-path-components (state "/path/to/")
                             (io/file "/path/to/subdir/file")) =>
    ["subdir" "file"]))
