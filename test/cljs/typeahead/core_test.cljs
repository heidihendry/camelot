(ns typeahead.core-test
  (:require [typeahead.core :as sut]
            [cljs.test :refer-macros [deftest is testing]]
            [clojure.set :as set]))

(defn ->no-context
  [term]
  {:term term})

(defn ->some-context
  [term]
  {:term term
   :props {:context "abc"}})

(deftest word-index-test
  (testing "Basic indexing"
    (testing "should index a single word"
      (is (= (sut/word-index (map ->no-context ["hello"]))
             {"h" {"e" {"l" {"l" {"o" {:props {}}}}}}})))

    (testing "should index two overlapping words"
      (is (= (sut/word-index (map ->no-context ["hello" "help"]))
             {"h" {"e" {"l" {"l" {"o" {:props {}}}
                             "p" {:props {}}}}}})))

    (testing "should index identically regardless of insertion order."
      (is (= (sut/word-index (map ->no-context ["help" "hello"]))
             {"h" {"e" {"l" {"l" {"o" {:props {}}}
                             "p" {:props {}}}}}})))

    (testing "should index two words without overlap."
      (is (= (sut/word-index (map ->no-context ["wildlife" "hello"]))
             {"w" {"i" {"l" {"d" {"l" {"i" {"f" {"e" {:props {}}}}}}}}}
              "h" {"e" {"l" {"l" {"o" {:props {}}}}}}})))

    (testing "should do blank string insertion should a longer string be defined."
      (is (= (sut/word-index (map ->no-context ["hello" "hell"]))
             {"h" {"e" {"l" {"l" {"o" {:props {}}
                                  "" {:props {}}}}}}})))

    (testing "should maintain the definition of a shorter string when defining a longer one."
      (is (= (sut/word-index (map ->no-context ["hell" "hello"]))
             {"h" {"e" {"l" {"l" {"o" {:props {}}
                                  "" {:props {}}}}}}})))

    (testing "should index a phrase as words, using word breaks as separators."
      (is (= (sut/word-index (map ->no-context ["spp:big bird"]))
             {"s" {"p" {"p" {:props {}}}}
              "b" {"i" {"g" {:props {}}
                        "r" {"d" {:props {}}}}}})))

    (testing "should accept alphanumeric characters."
      (is (= (sut/word-index (map ->no-context ["trap:10"]))
             {"t" {"r" {"a" {"p" {:props {}}}}}
              "1" {"0" {:props {}}}})))

    (testing "should be case insensitive."
      (is (= (sut/word-index (map ->no-context ["TRAP"]))
             {"t" {"r" {"a" {"p" {:props {}}}}}})))

    (testing "should produce the expected result for a series of similar words."
      (is (= (sut/word-index (map ->no-context ["the" "that" "then" "than" "hen" "thin"]))
             {"h" {"e" {"n" {:props {}}}}
              "t" {"h" {"e" {"" {:props {}}
                             "n" {:props {}}}
                        "i" {"n" {:props {}}}
                        "a" {"n" {:props {}}
                             "t" {:props {}}}}}}))))

  (testing "Context-aware indexing"
    (testing "should assign correct contexts where there are similar terms."
      (is (= (sut/word-index [(->no-context "help")
                              (->some-context "hello")])
             {"h" {"e" {"l" {"l" {"o" {:props {:context "abc"}}}
                             "p" {:props {}}}}}})))

    (testing "should assign correct contexts where there are existing substrings"
      (is (= (sut/word-index [(->no-context "hell")
                              (->some-context "hello")])
             {"h" {"e" {"l" {"l" {"o" {:props {:context "abc"}}
                                  "" {:props {}}}}}}})))

    (testing "should assign correct contexts when a substring exists"
      (is (= (sut/word-index [(->no-context "hello")
                              (->some-context "hell")])
             {"h" {"e" {"l" {"l" {"" {:props {:context "abc"}}
                                  "o" {:props {}}}}}}})))

    (testing "should produce the expected result for a series of similar words."
      (is (= (sut/word-index (apply conj
                                    (map ->no-context ["the" "that" "then"])
                                    (map ->some-context ["than" "hen" "thin"])))
             {"h" {"e" {"n" {:props {:context "abc"}}}}
              "t" {"h" {"e" {"" {:props {}}
                             "n" {:props {}}}
                        "i" {"n" {:props {:context "abc"}}}
                        "a" {"n" {:props {:context "abc"}}
                             "t" {:props {}}}}}})))))

(deftest phrase-index-test
  (testing "Should index phrases"
    (is (= (sut/phrase-index (map ->no-context ["hello: world"]))
           {"h" {"e" {"l" {"l" {"o" {":" {" " {"w" {"o" {"r" {"l" {"d" {:props {}}}}}}}}}}}}}}))))

(deftest complete-test
  (testing "Should find a single match in a one-term trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"o" {:props {}}}}}}} "hel")
           ["hello"])))

  (testing "Should support multiple completions in a simple trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"" {:props {}}
                                             "o" {:props {}}}}}}} "hel")
           ["hell" "hello"])))

  (testing "Should support multiple completions in a simple trie."
    (is (= (sut/complete {"h" {"e" {"l" {"l" {"" {:props {}}
                                             "o" {:props {}}}}}}} "hel")
           ["hell" "hello"])))

  (testing "Results are sorted alphabetically."
    (is (= (sut/complete {"x" {"y" {"z" {:props {}}}}
                         "a" {"b" {"c" {"e" {:props {}}
                                        "d" {:props {}}}}}} "")
           ["abcd" "abce" "xyz"])))

  (testing "Should be able to numerous completions in a more complex trie."
    (is (= (sut/complete {"h" {"e" {"n" {:props {}}}}
                         "t" {"h" {"e" {"" {:props {}}
                                        "n" {:props {}}}
                                   "i" {"n" {:props {}}}
                                   "a" {"n" {:props {}}
                                        "t" {:props {}}}}}} "")
           ["hen" "than" "that" "the" "then" "thin"])))

  (testing "Should complete phrases"
    (is (= (sut/complete
            {"h" {"e" {"l" {"l" {"o" {":" {" " {"w" {"o" {"r" {"l" {"d" {:props {}}}}}}}}}}}}}}
            "hello")
           ["hello: world"]))))

(deftest term-at-point-test
  (testing "term-at-point"
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
             "")))))

(deftest splice-test
  (testing "Should allow splice in the middle"
    (is (= (sut/splice (seq "hello world!") (seq "everybody") 6 11)
           (seq "hello everybody!"))))

  (testing "Should allow splice at the end"
    (is (= (sut/splice (seq "hello ") (seq "everybody") 6 6)
           (seq "hello everybody")))))

(deftest replace-term-test
  (testing "replace-term"
    (testing "Should replace the entire search if not multi-term"
      (is (= (sut/replace-term "hello world" 5 "replacement" "" false)
             "replacement")))

    (testing "Should perform basic replacement if multi-term"
      (is (= (sut/replace-term "hel" 3 "hello" " " true)
             "hello ")))

    (testing "Should replace term at point if multi-term"
      (is (= (sut/replace-term "hello world" 5 "replacement" "" true)
             "replacement world")))

    (testing "Should append replacement text if starting a new term"
      (is (= (sut/replace-term "hello " 6 "replacement" " " true)
             "hello replacement ")))

    (testing "Should be clever about replacing fields"
      (is (= (sut/replace-term "test:yes hello:" 10 "world" "" true)
             "test:yes world:")))

    (testing "Should allow arbitrary replacements"
      (is (= (sut/replace-term "test:yes hello:" 6 "yay" " " true)
             "test:yay  hello:")))))

(deftest ifind-test
  (testing "Find"
    (testing "should return properties for an exact match"
      (is (= (sut/ifind {"h" {"e" {"l" {"l" {"o" {:props {:context "xyz"}}
                                             "" {:props {:context "abc"}}}}}}}
                        "hello")
             {:context "xyz"})))

    (testing "should return property corresponding to the search string"
      (is (= (sut/ifind {"h" {"e" {"l" {"l" {"o" {:props {:context "xyz"}}
                                             "" {:props {:context "abc"}}}}}}}
                        "hell")
             {:context "abc"})))

    (testing "should return nil if match is not found"
      (is (= (sut/ifind {"h" {"e" {"l" {"l" {"o" {:props {:context "xyz"}}
                                             "" {:props {:context "abc"}}}}}}}
                        "world")
             nil)))))

(deftest field-context-test
  (testing "Field context"
    (testing "should return field context at point"
      (is (= (sut/field-context "field:hel" 9)
             "field")))

    (testing "should set context immediately following the field"
      (is (= (sut/field-context "field:" 6)
             "field")))

    (testing "should not set field context when within a field"
      (is (= (sut/field-context "field:" 5)
             nil)))

    (testing "should lose context once a space is inserted"
      (is (= (sut/field-context "field:hel " 10)
             nil)))

    (testing "should lose context once a pipe is inserted"
      (is (= (sut/field-context "field:hel|" 10)
             nil)))

    (testing "should be fine with an empty expression"
      (is (= (sut/field-context "" 0)
             nil)))

    (testing "should be fine with a single character expression"
      (is (= (sut/field-context "a" 1)
             nil)))))
