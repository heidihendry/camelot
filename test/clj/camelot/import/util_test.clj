(ns camelot.import.util-test
  (:require
   [camelot.import.util :refer :all]
   [camelot.test-util.state :as state]
   [clojure.test :refer :all]))

(deftest test-path-description
  (testing "Path Descriptions"
    (testing "Metadata can be described with a keyword matching corresponding to its path"
      (let [config {:language :en}]
        (is (= (path-description (state/gen-state config) [:camera :make]) "Camera Make"))
        (is (= (path-description (state/gen-state config) [:datetime]) "Date/Time"))))))
