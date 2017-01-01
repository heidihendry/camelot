(ns camelot.util.model-test
  (:require
   [camelot.util.model :as sut]
   #?(:clj
      [clojure.test :refer [deftest is testing use-fixtures]]
      :cljs
      [cljs.test :refer-macros [deftest is testing use-fixtures]])))

(defn sym-id
  [& vs]
  (identity (first vs)))

(deftest test-check-mapping
  (testing "Check mapping validity"
    (testing "should omit valid entry"
      (let [ps [{:test1 {:datatype :number :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{:required} :datatypes #{:number}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps) []))))

    (testing "should identify incorrect datatype"
      (let [ps [{:test1 {:datatype :number :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{:required} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/datatype-problem-only]))))

    (testing "should identify insufficient constraints"
      (let [ps [{:test1 {:datatype :number :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:number}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/required-constraint-problem-only]))))

    (testing "should identify if both datatype and constraints are incorrect"
      (let [ps [{:test1 {:datatype :number :required true}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/datatype-and-required-constraint-problem]))))

    (testing "should identify if schema not available"
      (let [ps [{:test1 {:datatype :number :required true}}
                {:test1 "Other"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/calculated-schema-not-available]))))

    (testing "should ignore columns without a mapping"
      (let [ps [{:test1 {:datatype :number :required true}
                 :test2 {:datatype :string :required false}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{:required} :datatypes #{:number}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps) []))))

    (testing "should return a result for each failed field"
      (let [ps [{:test1 {:datatype :number :required true}
                 :test2 {:datatype :string :required false}}
                {:test1 "Other"
                 :test2 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:other}}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/calculated-schema-not-available
                :camelot.util.model/datatype-problem-only]))))

    (testing "should flag max-length problem if it exceeds restriction."
      (let [ps [{:test1 {:datatype :string :max-length 5 :required false}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:string}
                            :max-length 10}}
                sym-id]]
        (is (= (apply sut/check-mapping ps)
               [:camelot.util.model/max-length-problem]))))

    (testing "should not flag max-length problem if not restricted."
      (let [ps [{:test1 {:datatype :string :required false}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:string}
                            :max-length 10}}
                sym-id]]
        (is (= (apply sut/check-mapping ps) []))))

    (testing "should not flag max-length problem if limit not exceeded."
      (let [ps [{:test1 {:datatype :string :max-length 10 :required false}}
                {:test1 "Column1"}
                {"Column1" {:constraints #{} :datatypes #{:string}
                            :max-length 10}}
                sym-id]]
        (is (= (apply sut/check-mapping ps) []))))))
