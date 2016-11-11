(ns camelot.util.data-test
  (:require
   [camelot.util.data :as sut]
   [clojure.test :refer :all]
   [clojure.set :as set]))

(deftest test-prefix-key
  (testing "Prefix key"
    (testing "Should nest single key when using one prefix."
      (is (= (sut/prefix-key {:data-one 1} [:data]) {:data {:one 1}})))

    (testing "Should nest all keys with the given prefix"
      (is (= (sut/prefix-key {:data-one 1 :data-two 2} [:data]) {:data {:one 1 :two 2}})))

    (testing "Should support nesting of multiple prefixes at once"
      (is (= (sut/prefix-key {:data-one 1 :test-one 1} [:data :test]) {:data {:one 1}
                                                                       :test {:one 1}})))

    (testing "Should supports prefixes which do not end in a hyphen"
      (is (= (sut/prefix-key {:datay-one 1 :testy-one 1} [:data :test]) {:data {:y-one 1}
                                                                         :test {:y-one 1}})))

    (testing "Should not nest keys which do not have a prefix"
      (is (= (sut/prefix-key {:data-one 1 :test-one 1} [:data]) {:data {:one 1}
                                                                 :test-one 1})))

    (testing "Should split on the longest possible prefix"
      (is (= (sut/prefix-key {:data-specific-one 1 :data-one 1}
                             [:data :data-specific]) {:data-specific {:one 1}
                                                      :data {:one 1}})))

    (testing "Should split on the longest possible prefix regardless of prefix ordering"
      (is (= (sut/prefix-key {:data-specific-one 1 :data-one 1}
                             [:data-specific :data]) {:data-specific {:one 1}
                                                      :data {:one 1}})))

    (testing "Should use nil as the key when a key is equal to a prefix"
      (is (= (sut/prefix-key {:data 1 :data-one 1}
                             [:data]) {:data {nil 1
                                              :one 1}})))))