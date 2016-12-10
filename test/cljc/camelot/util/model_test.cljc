(ns camelot.util.model-test
  (:require [camelot.util.model :as sut]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer :all :include-macros true])))

(defn sym-id
  [& vs]
  (identity (first vs)))

(deftest test-check-mapping
  (testing "Check mapping validity"
    (testing "should omit valid entry"
      (let [ps [{:test1 {:datatype :file :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{:required} :datatypes #{:file}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps) []))))

    (testing "should identify incorrect datatype"
      (let [ps [{:test1 {:datatype :file :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{:required} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/datatype-problem-only]))))

    (testing "should identify insufficient constraints"
      (let [ps [{:test1 {:datatype :file :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:file}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/required-constraint-problem-only]))))

    (testing "should identify if both datatype and constraints are incorrect"
      (let [ps [{:test1 {:datatype :file :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/datatype-and-required-constraint-problem]))))

    (testing "should identify if schema not available"
      (let [ps [{:test1 {:datatype :file :required true}}
                {:test1 "Other"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/calculated-schema-not-available]))))

    (testing "should return a result for each failed field"
      (let [ps [{:test1 {:datatype :file :required true}
                 :test2 {:datatype :string :required false}}
                {:test1 "Other"
                 :test2 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/calculated-schema-not-available
                :camelot.util.model/datatype-problem-only]))))))
