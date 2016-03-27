(ns camelot.problems-test
  (:require [midje.sweet :refer :all]
            [clojure.data :refer [diff]]
            [schema.test :as st]
            [camelot.config :refer [gen-state]]
            [camelot.problems :refer :all]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "highest severity"
       (fact "Error is highest severity"
             (reduce highest-severity :okay [:okay :error :ignore :warn :info]) => :error)

       (fact "Warn is second highest severity"
         (reduce highest-severity :okay [:okay :ignore :warn :info]) => :warn)

       (fact "Info is third highest severity"
         (reduce highest-severity :okay [:okay :info :ignore]) => :info)

       (fact "Ignore is higher severity than okay"
         (reduce highest-severity :okay [:okay :ignore]) => :ignore))

(defn test-problem-handler-helper
  [f level]
  (problem-handler
   (gen-state {:language :en})
   f
   nil
   level
   :problems/datetime))

(facts test-problem-handler
  (fact "Handler is ran when problem is not ignored"
    (let [v (atom 0)
          h (fn [a b c] (swap! v inc))]
      (test-problem-handler-helper h :info)
      @v => 1))

  (fact "Handler is not ran when problem is ignored"
    (let [v (atom 0)
          h (fn [a b c] (swap! v inc))]
      (test-problem-handler-helper h :ignore)
      @v = 0)))
