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

(deftest nil-search-returns-all
  (let [expected [{:media-id 1}]
        results [{:media-id 1}]]
    (is (= (only-matching nil results) expected))))

(deftest nil-search-returns-all
  (let [expected [{:media-id 1}]
        results [{:media-id 1}]]
    (is (= (only-matching nil results) expected))))

(deftest empty-search-returns-all
  (let [expected [{:media-id 1}]
        results [{:media-id 1}]]
    (is (= (only-matching "" results) expected))))

(deftest species-search-returns-records-with-matching-species
  (let [expected [(->record {:taxonomy-id 1
                             :media-id 2})]
        results [{:media-id 1}
                 (->record {:taxonomy-id 1
                          :media-id 2})]]
    (is (= (only-matching "Smiley" results) expected))))

(deftest species-search-is-case-insensitive
  (let [expected [(->record {:taxonomy-id 1
                             :media-id 2})]
        results [{:media-id 1}
                 (->record {:taxonomy-id 1
                            :media-id 2})]]
    (is (= (only-matching "smiley" results) expected))))

(deftest species-search-matches-middle-of-string
  (let [expected [(->record {:taxonomy-id 1
                             :media-id 2})]
        results [{:media-id 1}
                 (->record {:taxonomy-id 1
                            :media-id 2})]]
    (is (= (only-matching "ley wolf" results) expected))))

(deftest non-matching-species-are-ignored
  (let [expected [(->record {:taxonomy-id 1
                             :media-id 2})]
        results [(->record {:taxonomy-id 2
                            :media-id 1})
                 (->record {:taxonomy-id 1
                            :media-id 2})]]
    (is (= (only-matching "ley wolf" results) expected))))

(deftest search-by-site
  (let [expected [{:site-name "MySite"
                   :media-id 1}]
        results [{:site-name "MySite"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "someothersite"
                            :media-id 2})]]
    (is (= (only-matching "mysite" results) expected))))

(deftest matches-any-field
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

(deftest terms-separated-by-space-are-conjunctive
  (let [expected [(->record {:taxonomy-id 1
                             :site-name "MySite"
                             :media-id 2})]
        results [{:site-name "Wolf Den"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "MySite"
                            :media-id 2})]]
    (is (= (only-matching "wolf site" results) expected))))

(deftest terms-separated-by-pipe-are-disjunctive
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

(deftest allows-field-to-be-specified
  (let [expected [(->record {:taxonomy-id 1
                             :site-name "MySite"
                             :media-id 2})]
        results [{:site-name "Wolf Den"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "MySite"
                            :media-id 2})]]
    (is (= (only-matching "taxonomy-species:wolf" results) expected))))

(deftest allows-field-shorthand-for-species
  (let [expected [(->record {:taxonomy-id 1
                             :site-name "MySite"
                             :media-id 2})]
        results [{:site-name "Wolf Den"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "MySite"
                            :media-id 2})]]
    (is (= (only-matching "species:wolf" results) expected))))

(deftest allows-field-shorthand-for-site
  (let [expected [(->record {:taxonomy-id 1
                             :site-name "MySite"
                             :media-id 2})]
        results [{:site-name "Wolf Den"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "MySite"
                            :media-id 2})]]
    (is (= (only-matching "site:mysite" results) expected))))

(deftest allows-field-shorthand-for-camera
  (let [expected [{:camera-name "ABC01"
                   :media-id 1}]
        results [{:camera-name "ABC01"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :camera-name "XYZ3"
                            :media-id 2})]]
    (is (= (only-matching "camera:abc" results) expected))))

(deftest ignores-nonsense-fields
  (let [results [{:site-name "Wolf Den"
                  :media-id 1}
                 (->record {:taxonomy-id 1
                            :site-name "MySite"
                            :media-id 2})]]
    (is (= (only-matching "nonexisty:wolf" {}) '()))))

(deftest complex-searches-return-expected-results
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

(deftest finds-attention-needed-true-boolean
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

(deftest finds-media-attention-needed-false-boolean
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

(deftest finds-media-processed-true-boolean
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

(deftest finds-media-processed-false-boolean
  (let [expected [(->record {:taxonomy-id 1
                             :media-processed false
                             :media-id 2})]
        results [(->record {:taxonomy-id 1
                            :media-processed false
                            :media-id 2})
                 (->record {:taxonomy-id 2
                            :media-processed true
                            :media-id 1})]]
    (is (= (only-matching "processed:false" results) expected))))

(deftest trap-station-ids-are-matched-exactly
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

(deftest exact-matches-are-respected
  (let [search "sighting-sex:f"
        expected [(->record {:taxonomy-id 1
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
    (is (= (only-matching search results) expected))))

(deftest term-formatter-replaces-spaces
  (let [search "this is a test"]
    (is (= (sut-parser/format-terms search)
           "this+++is+++a+++test"))))

(deftest term-formatter-leaves-spaces-in-quotes-alone
  (let [search "this is \"a test\""]
    (is (= (sut-parser/format-terms search)
           "this+++is+++a test"))))
