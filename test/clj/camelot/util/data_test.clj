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

(deftest test-pair?
  (testing "pair?"
    (testing "Should return true if x is a pair-like thing"
      (are [value] (= (sut/pair? value) true)
        {:key "value"}
        [:key "value"]
        [1 2]
        (list 'a 'b)
        (cons 'a (cons 'b nil))))

    (testing "Should return false if x is not a pair-like thing"
      (are [value] (= (sut/pair? value) false)
        {:key "value" :key2 "value2"}
        [:key "value" :key2 "value2"]
        [1]
        (list 'a 'b 'c)
        (list 'a)
        (cons 'a nil)
        (cons 'a (cons 'b (cons 'c nil)))
        "ab"
        nil))))

(deftest test-map-val
  (testing "map-val"
    (testing "Should map over a hash-map appyling k and v to f"
      (let [data {:key "value"
                  :key2 "value2"}
            calls (atom [])]
        (sut/map-val (fn [v] (swap! calls #(conj % v))) data)
        (is (= @calls ["value" "value2"]))))

    (testing "Should return the result as a hash-map, keyed by the original value"
      (let [data {:key "value"
                  :key2 "value2"}
            result (sut/map-val (fn [v] (str v "!")) data)]
        (is (= result {:key "value!" :key2 "value2!"}))))

    (testing "Should work for pairs as well"
      (let [data [[:key "value"]
                  [:key2 "value2"]]
            result (sut/map-val (fn [v] (str v "!")) data)]
        (is (= result {:key "value!" :key2 "value2!"}))))

    (testing "Should throw for illegal coll input"
      (are [input] (thrown? IllegalArgumentException (sut/map-val :id input))
        "hello"
        1
        \c))

    (testing "Should throw if f is not a function"
      (is (thrown? AssertionError (sut/map-val 1 []))))))

(deftest test-key-by
  (testing "key-by"
    (testing "Should return a map keyed by the result of applying `f`."
      (let [data [{:id 1} {:id 2} {:id 3}]]
        (is (= (sut/key-by :id data)
               {1 {:id 1}
                2 {:id 2}
                3 {:id 3}}))))

    (testing "Should return only one match if duplicates exist."
      (let [data [{:id 1} {:id 2} {:id 3} {:id 3}]]
        (is (= (sut/key-by :id data)
               {1 {:id 1}
                2 {:id 2}
                3 {:id 3}}))))

    (testing "Should return the first match for duplicates."
      (let [data [{:id 1 :value "one"}
                  {:id 1 :value "two"}
                  {:id 1 :value "another"}]]
        (is (= (sut/key-by :id data)
               {1 {:id 1 :value "one"}}))))

    (testing "Should return empty map for empty coll"
      (is (= (sut/key-by :id []) {})))

    (testing "Should return empty map for nil"
      (is (= (sut/key-by :id nil) {})))

    (testing "Should throw for illegal coll input"
      (are [input] (thrown? IllegalArgumentException (sut/key-by :id input))
        "hello"
        1
        \c))

    (testing "Should throw if f is not a function"
      (is (thrown? AssertionError (sut/key-by 1 []))))))
