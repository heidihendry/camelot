(ns typeahead.core-test
  (:require [typeahead.core :as sut]
            [cljs.test :refer-macros [deftest is testing]]
            [clojure.set :as set]))

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

(deftest complete-test
  (testing "Should find a single match in a one-term trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"o" nil}}}}} "hel")
           ["hello"])))

  (testing "Should support multiple completions in a simple trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"" nil
                                             "o" nil}}}}} "hel")
           ["hell" "hello"])))

  (testing "Should support multiple completions in a simple trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"" nil
                                             "o" nil}}}}} "hel")
           ["hell" "hello"])))

  (testing "Results are sorted by length, then alphabetically."
    (is (= (sut/complete {"x" {"y" {"z" nil}}
                         "a" {"b" {"c" {"e" nil
                                        "d" nil}}}} "")
           ["xyz" "abcd" "abce"])))

  (testing "Should be able to numerous completions in a more complex trie."
    (is (= (sut/complete {"h" {"e" {"n" nil}}
                         "t" {"h" {"e" {"" nil
                                        "n" nil}
                                   "i" {"n" nil}
                                   "a" {"n" nil
                                        "t" nil}}}} "")
           ["hen" "the" "than" "that" "then" "thin"])))

  (testing "Should complete phrases"
    (is (= (sut/complete
            {"h" {"e" {"l" {"l" {"o" {":" {" " {"w" {"o" {"r" {"l" {"d" nil}}}}}}}}}}}}
            "hello")
           ["hello: world"]))))

(deftest term-at-point-test
  (testing "Should cater for empty value"
    (is (= (sut/term-at-point "" 0)
           "")))

  (testing "Should support term with caret at end of string"
    (is (= (sut/term-at-point "hello" 5)
           "hello")))

  (testing "Should support term with caret at beginning of string"
    (is (= (sut/term-at-point "hello" 0)
           "")))

  (testing "Should support term with caret in middle of string"
    (is (= (sut/term-at-point "hello" 3)
           "hello")))

  (testing "Should support multiple terms"
    (is (= (sut/term-at-point "hello wor" 9)
           "wor")))

  (testing "Should not find any term when starting a new one"
    (is (= (sut/term-at-point "hello " 6)
           "")))

  (testing "Should not find any term when immediately before another term"
    (is (= (sut/term-at-point "hello world" 6)
           "")))

  (testing "Should consider a field to be a term at point."
    (is (= (sut/term-at-point "species:" 6)
           "species")))

  (testing "Should not consider a field to be a term at point when immediately after it."
    (is (= (sut/term-at-point "species:" 8)
           ""))))

(deftest splice-test
  (testing "Should allow splice in the middle"
    (is (= (sut/splice (seq "hello world!") (seq "everybody") 6 11)
           (seq "hello everybody!"))))

  (testing "Should allow splice at the end"
    (is (= (sut/splice (seq "hello ") (seq "everybody") 6 6)
           (seq "hello everybody")))))

(deftest replace-term-test
  (testing "Should replace the entire search if not multi-term"
    (is (= (sut/replace-term "hello world" 5 "replacement" false)
           "replacement")))

  (testing "Should perform basic replacement if multi-term"
    (is (= (sut/replace-term "hel" 3 "hello" true)
           "hello")))

  (testing "Should replace term at point if multi-term"
    (is (= (sut/replace-term "hello world" 5 "replacement" true)
           "replacement world")))

  (testing "Should append replacement text if starting a new term"
    (is (= (sut/replace-term "hello " 6 "replacement" true)
           "hello replacement")))

  (testing "Should be clever about replacing fields"
    (is (= (sut/replace-term "test:yes hello:" 10 "world:" true)
           "test:yes world:"))))
