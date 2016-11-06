(ns camelot.util.data-test
  (:require [camelot.util.data :as sut]
            [midje.sweet :refer :all]
            [clojure.set :as set]))

(facts "Prefix key"
  (fact "Should nest single key when using one prefix."
    (sut/prefix-key {:data-one 1} [:data]) => {:data {:one 1}})

  (fact "Should nest all keys with the given prefix"
    (sut/prefix-key {:data-one 1 :data-two 2} [:data]) => {:data {:one 1 :two 2}})

  (fact "Should support nesting of multiple prefixes at once"
    (sut/prefix-key {:data-one 1 :test-one 1} [:data :test]) => {:data {:one 1}
                                                                 :test {:one 1}})

  (fact "Should supports prefixes which do not end in a hyphen"
    (sut/prefix-key {:datay-one 1 :testy-one 1} [:data :test]) => {:data {:y-one 1}
                                                                   :test {:y-one 1}})

  (fact "Should not nest keys which do not have a prefix"
    (sut/prefix-key {:data-one 1 :test-one 1} [:data]) => {:data {:one 1}
                                                           :test-one 1})

  (fact "Should split on the longest possible prefix"
    (sut/prefix-key {:data-specific-one 1 :data-one 1}
                    [:data :data-specific]) => {:data-specific {:one 1}
                                                :data {:one 1}})

  (fact "Should split on the longest possible prefix regardless of prefix ordering"
    (sut/prefix-key {:data-specific-one 1 :data-one 1}
                    [:data-specific :data]) => {:data-specific {:one 1}
                                                :data {:one 1}})

  (fact "Should use nil as the key when a key is equal to a prefix"
    (sut/prefix-key {:data 1 :data-one 1}
                    [:data]) => {:data {nil 1
                                        :one 1}}))
