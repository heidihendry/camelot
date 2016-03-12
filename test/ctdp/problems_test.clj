(ns ctdp.problems-test
  (:require [clojure.test :refer :all]
            [clojure.data :refer [diff]]
            [ctdp.problems :refer :all]))

(deftest test-highest-severity
  (testing "Error is highest severity"
    (is (= :error (reduce highest-severity :okay [:okay :error :ignore :warn :info]))))

  (testing "Warn is second highest severity"
    (is (= :warn (reduce highest-severity :okay [:okay :ignore :warn :info]))))

  (testing "Info is third highest severity"
    (is (= :info (reduce highest-severity :okay [:okay :info :ignore]))))

  (testing "Ignore is higher severity than okay"
    (is (= :ignore (reduce highest-severity :okay [:okay :ignore])))))

(defn test-problem-handler-helper
  [f level]
  (problem-handler
   { :translations (fn [a b] a)
    :config {:language :en}}
   f
   nil
   level
   :aproblem))

(deftest test-problem-handler
  (testing "Handler is ran when problem is not ignored"
    (let [v (atom 0)
          h (fn [a b c] (swap! v inc))]
      (test-problem-handler-helper h :info)
      (is (= @v 1))))

  (testing "Handler is not ran when problem is ignored"
    (let [v (atom 0)
          h (fn [a b c] (swap! v inc))]
      (test-problem-handler-helper h :ignore)
      (is (= @v 0)))))
