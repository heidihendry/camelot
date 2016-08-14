(ns camelot.component.albums-test
  (:require [camelot.component.albums :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest compare-validity-test
  (testing "Should consider order as :pass, :warn, :fail"
    (let [rs [{:result :pass}
              {:result :fail}
              {:result :warn}
              {:result :fail}
              {:result :warn}
              {:result :pass}]]
      (is (= (map :result (sort sut/compare-validity rs))
             [:pass :pass :warn :warn :fail :fail]))))

  (testing "Should order unknown things last"
    (let [rs [{:result :pass}
              {:result :fail}
              {:result :other}
              {:result :warn}
              {:result :fail}
              {:result :warn}
              {:result :pass}]]
      (is (= (map :result (sort sut/compare-validity rs))
             [:pass :pass :warn :warn :fail :fail :other]))))

  (testing "Sort should be stable"
    (let [rs [{:result :pass :n 1}
              {:result :fail :n 2}
              {:result :warn :n 3}
              {:result :fail :n 4}
              {:result :warn :n 5}
              {:result :pass :n 6}]]
      (is (= (sort sut/compare-validity rs)
             [{:result :pass :n 1}
              {:result :pass :n 6}
              {:result :warn :n 3}
              {:result :warn :n 5}
              {:result :fail :n 2}
              {:result :fail :n 4}])))))

(deftest compare-album-validity
  (testing "Should consider order :pass, :warn, :fail"
    (let [rs [[:b {:problems [{:result :warn}]}]
              [:c {:problems [{:result :fail}]}]
              [:a {:problems [{:result :pass}]}]]]
      (= (sort sut/compare-album-validity rs)
         [{:a {:problems [{:result :pass}]}}
          {:b {:problems [{:result :warn}]}}
          {:c {:problems [{:result :fail}]}}])))

  (testing "Something with 3 warnings is better than something with a failure."
    (let [rs [[:a {:problems [{:result :warn}
                              {:result :warn}
                              {:result :warn}]}]
              [:b {:problems [{:result :fail}]}]]]
      (= (sort sut/compare-album-validity rs)
         [{:a {:problems [{:result :warn}
                          {:result :warn}
                          {:result :warn}]}}
          {:b {:problems [{:result :fail}]}}])))

  (testing "Unknown results are sorted last."
    (let [rs [[:a {:problems [{:result :warn}
                              {:result :other}
                              {:result :warn}]}]
              [:b {:problems [{:result :fail}]}]
              [:c {:problems [{:result :other}]}]
              [:d {:problems [{:result :pass}]}]]]
      (= (sort sut/compare-album-validity rs)
         [{:d {:problems [{:result :pass}]}}
          {:b {:problems [{:result :fail}]}}
          {:a {:problems [{:result :warn}
                          {:result :other}
                          {:result :warn}]}}
          [:c {:problems [{:result :other}]}]])))

  (testing "Sort should be stable."
    (let [rs [[:a {:problems [{:result :pass}]}]
              [:b {:problems [{:result :fail}]}]
              [:c {:problems [{:result :warn}]}]
              [:d {:problems [{:result :fail}]}]
              [:e {:problems [{:result :warn}]}]
              [:f {:problems [{:result :pass}]}]]]
      (= (sort sut/compare-album-validity rs)
         [[:a {:problems [{:result :pass}]}]
          [:f {:problems [{:result :pass}]}]
          [:c {:problems [{:result :warn}]}]
          [:e {:problems [{:result :warn}]}]
          [:b {:problems [{:result :fail}]}]
          [:d {:problems [{:result :fail}]}]]))))
