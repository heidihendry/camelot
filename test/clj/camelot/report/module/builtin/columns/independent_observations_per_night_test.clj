(ns camelot.report.module.builtin.columns.independent-observations-per-night-test
  (:require [camelot.report.module.builtin.columns.independent-observations-per-night :as sut]
            [clojure.test :refer :all]
            [camelot.testutil.state :as state]))

(defn calculate
  [data]
  (sut/calculate-independent-observations-per-night (state/gen-state {}) data))

(deftest test-calculate-independent-observations-per-night
  (testing "calculate-independent-observations-per-night"

    (testing "Should assoc column to nil if independent-observations is nil"
      (let [data [{}]]
        (is (= (calculate data)
               [{:independent-observations-per-night nil}]))))

    (testing "Should assoc column to "-" if total-nights is nil"
      (let [data [{:independent-observations 10}]]
        (is (= (calculate data)
               [{:independent-observations 10
                 :independent-observations-per-night "-"}]))))

    (testing "Should assoc column to "-" if total-nights is nil"
       (let [data [{:independent-observations 10
                    :total-nights 0}]]
         (is (= (calculate data)
                [{:independent-observations 10
                  :total-nights 0
                  :independent-observations-per-night "-"}]))))

    (testing "Should divide indep. obs by total nights"
       (let [data [{:independent-observations 10
                    :total-nights 10}]]
         (is (= (calculate data)
                [{:independent-observations 10
                  :total-nights 10
                  :independent-observations-per-night "100.000"}]))))

    (testing "Should round division to 3 d.p."
       (let [data [{:independent-observations 1
                    :total-nights 7}]]
         (is (= (calculate data)
                [{:independent-observations 1
                  :total-nights 7
                  :independent-observations-per-night "14.286"}]))))))
