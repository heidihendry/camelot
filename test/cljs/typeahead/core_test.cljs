(ns camelot.component.albums-test
  (:require [typeahead.core :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest word-index-test
  (testing "Should index a single word"
    (is (= (sut/word-index ["hello"])
           {"h" {"e" {"l" {"l" {"o" nil}}}}})))

  (testing "Should index two overlapping words"
    (is (= (sut/word-index ["hello" "help"])
           {"h" {"e" {"l" {"l" {"o" nil}
                           "p" nil}}}})))

  (testing "Should index identically regardless of insertion order."
    (is (= (sut/word-index ["help" "hello"])
           {"h" {"e" {"l" {"l" {"o" nil}
                           "p" nil}}}})))

  (testing "Should index two words without overlap."
    (is (= (sut/word-index ["wildlife" "hello"])
           {"w" {"i" {"l" {"d" {"l" {"i" {"f" {"e" nil}}}}}}}
            "h" {"e" {"l" {"l" {"o" nil}}}}})))

  (testing "Should do blank string insertion should a longer string be defined."
    (is (= (sut/word-index ["hello" "hell"])
           {"h" {"e" {"l" {"l" {"o" nil
                                "" nil}}}}})))

  (testing "Should maintain the definition of a shorter string when defining a longer one."
    (is (= (sut/word-index ["hell" "hello"])
           {"h" {"e" {"l" {"l" {"o" nil
                                "" nil}}}}})))

  (testing "Should index a phrase as words, using word breaks as separators."
    (is (= (sut/word-index ["spp:big bird"])
           {"s" {"p" {"p" nil}}
            "b" {"i" {"g" nil
                      "r" {"d" nil}}}})))

  (testing "Should accept alphanumeric characters."
    (is (= (sut/word-index ["TRAP:10"])
           {"T" {"R" {"A" {"P" nil}}}
            "1" {"0" nil}})))

  (testing "Should produce the expected result for a series of similar words."
    (is (= (sut/word-index ["the" "that" "then" "than" "hen" "thin"])
           {"h" {"e" {"n" nil}}
            "t" {"h" {"e" {"" nil
                           "n" nil}
                      "i" {"n" nil}
                      "a" {"n" nil
                           "t" nil}}}}))))

(deftest phrase-index-test
  (testing "Should index phrases"
    (is (= (sut/phrase-index ["hello: world"])
           {"h" {"e" {"l" {"l" {"o" {":" {" " {"w" {"o" {"r" {"l" {"d" nil}}}}}}}}}}}}))))
