(ns camelot.library.filter-test
  (:require
   [camelot.library.filter :as sut]
   [camelot.library.filter-parser :as sut-parser]
   [clojure.test :refer [deftest is testing]]))

(def species {1 {:taxonomy-id 1
                 :taxonomy-species "Wolf"
                 :taxonomy-genus "Smiley"
                 :taxonomy-label "Smiley Wolf"}
              2 {:taxonomy-id 2
                 :taxonomy-genus "Yellow"
                 :taxonomy-species "Spotted Cat"
                 :taxonomy-label "Yellow Spotted Cat"}})

(defn ->record
  [record]
  (merge (or (get species (:taxonomy-id record)) {}) record))

(defn only-matching
  [terms records]
  (sut/only-matching (sut-parser/parse terms) records))

(deftest test-only-matching
  (testing "only-matching"
    (testing "should return all when nil searched"
      (let [expected [{:media-id 1}]
            results [{:media-id 1}]]
        (is (= (only-matching nil results) expected))))

    (testing "should return all when empty string searched"
      (let [expected [{:media-id 1}]
            results [{:media-id 1}]]
        (is (= (only-matching "" results) expected))))

    (testing "should return records with matching species"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-id 2})]
            results [{:media-id 1}
                     (->record {:taxonomy-id 1
                                :media-id 2})]]
        (is (= (only-matching "Smiley" results) expected))))

    (testing "should handle negation on string searches"
      (let [expected [{:media-id 1}]
            results [{:media-id 1}
                     (->record {:taxonomy-id 1
                                :media-id 2})]]
        (is (= (only-matching "!Smiley" results) expected))))

    (testing "should not be case sensitive"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-id 2})]
            results [{:media-id 1}
                     (->record {:taxonomy-id 1
                                :media-id 2})]]
        (is (= (only-matching "smiley" results) expected))))

    (testing "should be able to match middle of strings"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-id 2})]
            results [{:media-id 1}
                     (->record {:taxonomy-id 1
                                :media-id 2})]]
        (is (= (only-matching "ley wolf" results) expected))))

    (testing "should omit species which are not matched"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-id 2})]
            results [(->record {:taxonomy-id 2
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-id 2})]]
        (is (= (only-matching "ley wolf" results) expected))))

    (testing "should be able to search sites"
      (let [expected [{:site-name "MySite"
                       :media-id 1}]
            results [{:site-name "MySite"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "someothersite"
                                :media-id 2})]]
        (is (= (only-matching "mysite" results) expected))))

    (testing "should be able to search sites"
      (let [expected [(->record {:taxonomy-id 1
                                :site-name "someothersite"
                                :media-id 2})]
            results [{:site-name "MySite"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "someothersite"
                                :media-id 2})]]
        (is (= (only-matching "!mysite" results) expected))))

    (testing "should be able to match on any field with plain string search"
      (let [expected [{:site-name "Wolf Den"
                       :media-id 1}
                      (->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "wolf" results) expected))))

    (testing "should treat space (' ') as a conjunction operator"
      (let [expected [(->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "wolf site" results) expected))))

    (testing "should treat pipe ('|') as a disjunction operator"
      (let [expected [{:site-name "Wolf Den"
                       :media-id 1}
                      (->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})
                     (->record {:taxonomy-id 2
                                :site-name "Excluded"
                                :media-id 3})]]
        (is (= (only-matching "wolf site|den" results) expected))))

    (testing "should be able to match on specific field names"
      (let [expected [(->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "taxonomy-species:wolf" results) expected))))

    (testing "should handle negation on string field searches"
      (let [expected [{:site-name "Wolf Den"
                      :media-id 1}]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "!taxonomy-species:wolf" results) expected))))

    (testing "should be able to match on calculated field names"
      (let [expected [(->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "species:wolf" results) expected))))

    (testing "should permit field alias for site"
      (let [expected [(->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "site:mysite" results) expected))))

    (testing "should permit field alias for camera"
      (let [expected [{:camera-name "ABC01"
                       :media-id 1}]
            results [{:camera-name "ABC01"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :camera-name "XYZ3"
                                :media-id 2})]]
        (is (= (only-matching "camera:abc" results) expected))))

    (testing "should ignore non-existent field names"
      (let [results [{:site-name "Wolf Den"
                      :media-id 1}
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching "nonexisty:wolf" {}) '()))))

    (testing "should return expected results for complex searches"
      (let [expected [(->record {:taxonomy-id 1
                                 :site-name "MySite"
                                 :media-id 2})]
            results [(->record {:taxonomy-id 2
                                :site-name "Wolf Den"
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :site-name "MySite"
                                :media-id 2})]]
        (is (= (only-matching
                "taxonomy-species:wolf mysite|taxonomy-species:cat mysite"
                results) expected))))

    (testing "should allow matching on attention-needed:true"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-attention-needed true
                                 :media-id 2})]
            results [(->record {:taxonomy-id 2
                                :media-attention-needed false
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-attention-needed true
                                :media-id 2})]]
        (is (= (only-matching "flagged:true" results) expected))))

    (testing "should allow matching on attendion-needed:false"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-attention-needed false
                                 :media-id 2})]
            results [(->record {:taxonomy-id 1
                                :media-attention-needed false
                                :media-id 2})
                     (->record {:taxonomy-id 2
                                :media-attention-needed true
                                :media-id 1})]]
        (is (= (only-matching "flagged:false" results) expected))))

    (testing "should allow matching on processed:true"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-processed true
                                 :media-id 2})]
            results [(->record {:taxonomy-id 2
                                :media-processed false
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-processed true
                                :media-id 2})]]
        (is (= (only-matching "processed:true" results) expected))))

    (testing "should allow matching on processed:false"
      (let [expected [(->record {:taxonomy-id 2
                                 :media-processed false
                                 :media-id 1})]
            results [(->record {:taxonomy-id 2
                                :media-processed false
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-processed true
                                :media-id 2})]]
        (is (= (only-matching "processed:false" results) expected))))

    (testing "should allow negation on false boolean field values"
      (let [expected [(->record {:taxonomy-id 1
                                 :media-processed true
                                 :media-id 2})]
            results [(->record {:taxonomy-id 2
                                :media-processed false
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-processed true
                                :media-id 2})]]
        (is (= (only-matching "!processed:false" results) expected))))

    (testing "should allow negation on true boolean field values"
      (let [expected [(->record {:taxonomy-id 2
                                :media-processed false
                                :media-id 1})]
            results [(->record {:taxonomy-id 2
                                :media-processed false
                                :media-id 1})
                     (->record {:taxonomy-id 1
                                :media-processed true
                                :media-id 2})]]
        (is (= (only-matching "!processed:true" results) expected))))

    (testing "should match IDs exactly"
      (let [expected [(->record {:taxonomy-id 2
                                 :trap-station-id 5
                                 :media-id 1})]
            results [(->record {:taxonomy-id 1
                                :trap-station-id 50
                                :media-id 2
                                :media-processed true})
                     (->record {:taxonomy-id 2
                                :trap-station-id 5
                                :media-id 1})]]
        (is (= (only-matching "trapid:5" results) expected))))

    (testing "should respect exact matches"
      (let [expected [(->record {:taxonomy-id 1
                                 :sighting-sex "F"
                                 :media-id 2
                                 :media-processed true})]
            results [(->record {:taxonomy-id 1
                                :sighting-sex "F"
                                :media-id 2
                                :media-processed true})
                     (->record {:taxonomy-id 2
                                :sighting-sex "unidentified"
                                :media-id 1
                                :media-processed false})
                     (->record {:taxonomy-id 3
                                :sighting-sex nil
                                :media-id 3
                                :media-processed false})]]
        (is (= (only-matching "sighting-sex:f" results) expected))))))

(deftest term-formatter-replaces-spaces
  (let [search "this is a test"]
    (is (= (sut-parser/format-terms search)
           "this+++is+++a+++test"))))

(deftest term-formatter-leaves-spaces-in-quotes-alone
  (let [search "this is \"a test\""]
    (is (= (sut-parser/format-terms search)
           "this+++is+++a test"))))
