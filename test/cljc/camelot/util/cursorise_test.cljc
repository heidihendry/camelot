(ns camelot.util.cursorise-test
  (:require
   [camelot.util.cursorise :as sut]
   #?(:clj [clojure.test :refer [deftest is testing use-fixtures]]
      :cljs [cljs.test :refer-macros [deftest is testing use-fixtures]])
   [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest test-decursorise
  (testing "Decursorise"
    (testing "Should elevate leaves of :value nodes."
      (is (= (sut/decursorise {:akey {:value 1}}) {:akey 1})))

    (testing "Should ignore paths not containing a :value key."
      (is (= (sut/decursorise {:akey 1}) {:akey 1})))

    (testing "Should handle arbitrarily deep paths containing a :value key."
      (is (= (sut/decursorise {:akey {:anotherkey {:nextkey {:value 1}}}})
             {:akey {:anotherkey {:nextkey 1}}})))

    (testing "Should be able to mix between paths with and without :value keys."
      (is (= (sut/decursorise {:akey 1
                               :anotherkey {:value 1}})
             {:akey 1 :anotherkey 1})))

    (testing "Should be idempotent"
      (is (= (sut/decursorise (sut/decursorise {:akey 1
                                                :anotherkey {:value 1}}))
             {:akey 1 :anotherkey 1})))))

(deftest test-cursorise
  (testing "Cursorise"
    (testing "Should add :value nodes where missing."
      (is (= (sut/cursorise {:akey 1}) {:akey {:value 1}})))

    (testing "Should ignore paths not containing a :value key."
      (is (= (sut/cursorise {:akey {:value 1}}) {:akey {:value 1}})))

    (testing "Should handle arbitrarily deep paths containing a :value key."
      (is (= (sut/cursorise {:akey {:anotherkey {:nextkey 1}}})
             {:akey {:anotherkey {:nextkey {:value 1}}}})))

    (testing "Should be able to mix between paths with and without :value keys."
      (is (= (sut/cursorise {:akey 1
                             :anotherkey {:value 1}})
             {:akey {:value 1}
              :anotherkey {:value 1}})))

    (testing "Should be idempotent"
      (is (= (sut/cursorise (sut/cursorise {:akey 1
                                            :anotherkey {:value 1}}))
             {:akey {:value 1}
              :anotherkey {:value 1}})))))
