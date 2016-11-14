(ns camelot.util.data-test
  (:require
   [camelot.util.data :as sut]
   [clojure.test :refer :all]
   [clojure.set :as set]))

(deftest test-key-prefix-to-map
  (testing "Prefix key to map transform"
    (testing "Should nest single key when using one prefix."
      (is (= (sut/key-prefix-to-map {:data-one 1} [:data]) {:data {:one 1}})))

    (testing "Should nest all keys with the given prefix"
      (is (= (sut/key-prefix-to-map {:data-one 1 :data-two 2} [:data]) {:data {:one 1 :two 2}})))

    (testing "Should support nesting of multiple prefixes at once"
      (is (= (sut/key-prefix-to-map {:data-one 1 :test-one 1} [:data :test]) {:data {:one 1}
                                                                       :test {:one 1}})))

    (testing "Should supports prefixes which do not end in a hyphen"
      (is (= (sut/key-prefix-to-map {:datay-one 1 :testy-one 1} [:data :test]) {:data {:y-one 1}
                                                                         :test {:y-one 1}})))

    (testing "Should not nest keys which do not have a prefix"
      (is (= (sut/key-prefix-to-map {:data-one 1 :test-one 1} [:data]) {:data {:one 1}
                                                                 :test-one 1})))

    (testing "Should split on the longest possible prefix"
      (is (= (sut/key-prefix-to-map {:data-specific-one 1 :data-one 1}
                             [:data :data-specific]) {:data-specific {:one 1}
                                                      :data {:one 1}})))

    (testing "Should split on the longest possible prefix regardless of prefix ordering"
      (is (= (sut/key-prefix-to-map {:data-specific-one 1 :data-one 1}
                             [:data-specific :data]) {:data-specific {:one 1}
                                                      :data {:one 1}})))

    (testing "Should use nil as the key when a key is equal to a prefix"
      (is (= (sut/key-prefix-to-map {:data 1 :data-one 1}
                             [:data]) {:data {nil 1
                                              :one 1}})))

    (testing "Should reverse map-keys-to-key-prefix"
      (let [data {:data {:one 1 :two 2}}
            result (-> data
                       (sut/map-keys-to-key-prefix [:data])
                       (sut/key-prefix-to-map [:data]))]
        (is (= result data))))))

(deftest test-map-keys-to-key-prefix
  (testing "Utility to map keys to key prefix"
    (testing "Should reverse key-to-prefix-map"
      (let [data {:data-specific-one 1 :data-one 1}
            result (-> data
                       (sut/key-prefix-to-map [:data])
                       (sut/map-keys-to-key-prefix [:data]))]
        (is (= result data))))))
