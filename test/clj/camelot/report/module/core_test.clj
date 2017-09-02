(ns camelot.report.module.core-test
  (:require [camelot.report.core :as sut]
            [clojure.test :refer :all]))

(def sample-data
  [{:field-individual "Bruce"}
   {:field-confidence 10}
   {:field-individual "Jerry"
    :field-confidence 5}
   {}
   {:other true}])

(def built-in-column-count 61)

(deftest test-expand-cols
  (testing "expand-cols"
    (testing "Should return original columns if no wildcard columns included"
      (is (= (sut/expand-cols sample-data [] [:col1 :col2]) [:col1 :col2])))

    (testing "Should return all columns if :all keyword used in first position"
      (is (= (count (sut/expand-cols sample-data [] [:all])) built-in-column-count)))

    (testing "Should not use sighting field columns if not in column list"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "individual"
                                            :sighting-field-ordering 10}]
                              [:col1 :col2]) [:col1 :col2])))

    (testing "Should use the known sighting fields, even if more sighting fields in dataset"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "individual"
                                            :sighting-field-ordering 10}]
                              [:col1 :col2 :all-fields]) [:col1 :col2 :field-individual])))

    (testing "Should include all sighting fields known and present in dataset"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "individual"
                                            :sighting-field-ordering 10}
                                           {:sighting-field-key "confidence"
                                            :sighting-field-ordering 5}]
                              [:col1 :col2 :all-fields]) [:col1 :col2 :field-confidence :field-individual])))

    (testing "Should sort by order weighting defined on the sighting field"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "individual"
                                            :sighting-field-ordering 10}
                                           {:sighting-field-key "confidence"
                                            :sighting-field-ordering 99}]
                              [:col1 :col2 :all-fields]) [:col1 :col2 :field-individual :field-confidence])))

    (testing "Should include sighting fields when using :all wildcard"
      (is (= (count (sut/expand-cols sample-data [{:sighting-field-key "individual"
                                                   :sighting-field-ordering 10}
                                                  {:sighting-field-key "confidence"
                                                   :sighting-field-ordering 99}]
                                     [:all])) (+ ^long built-in-column-count 2))))

    (testing "Should not include sighting fields where data is missing from dataset"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "missing"
                                            :sighting-field-ordering 10}]
                              [:col1 :col2 :all-fields]) [:col1 :col2])))

    (testing "Should include sighting fields even if missing from the dataset if explicitly included"
      (is (= (sut/expand-cols sample-data [{:sighting-field-key "missing"
                                            :sighting-field-ordering 10}]
                              [:col1 :col2 :field-missing]) [:col1 :col2 :field-missing])))))

(deftest test-aggregate-data
  (testing "aggregate-data"
    (testing "should not treat nil specially"
      (letfn [(redfn [s col grp] (reduce + 0 (map #(or (get % col) 0) grp)))
              (agg-reducer [s grp acc col] (assoc acc col (redfn s col grp)))]
        (with-redefs [sut/aggregate-reducer agg-reducer]
          (let [data [{:aggcol1 1
                       :aggcol2 2
                       :anchor 1}
                      {:aggcol1 1
                       :aggcol2 2
                       :anchor 2}
                      {:aggcol1 10
                       :aggcol2 nil
                       :anchor 1}]
                state {}
                cols [:aggcol1 :aggcol2 :anchor]
                agg-cols [:aggcol1 :aggcol2]]
            (is (= (sut/aggregate-data state cols agg-cols data)
                   [{:aggcol1 11
                     :aggcol2 2
                     :anchor 1}
                    {:aggcol1 11
                     :aggcol2 2
                     :anchor 1}
                    {:aggcol1 1
                     :aggcol2 2
                     :anchor 2}]))))))))
