(ns camelot.import.bulk-test
  (:require [camelot.import.bulk :as sut]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]
            [clojure.test :refer :all]
            [camelot.util.datatype :as datatype]))

(deftest test-file-data-to-record-list
  (testing "transforming file data to records"
    (testing "mapping to null is omitted"
      (with-redefs [datatype/deserialise-field (fn [k d] d)]
        (is (= (sut/file-data-to-record-list (state/gen-state)
                                             [["V1-1","V1-2","V1-3"]
                                              ["V2-1","V2-2","V2-3"]]
                                             {"H1" 0 "H2" 1 "H3" 2}
                                             {:h1 "H1" :h2 nil :h3 "H3"})
               [{:h1 "V1-1" :h3 "V1-3"}
                {:h1 "V2-1" :h3 "V2-3"}]))))))
