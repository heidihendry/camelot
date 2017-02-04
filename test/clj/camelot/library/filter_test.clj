(ns camelot.library.filter-test
  (:require
   [camelot.library.filter :as sut]
   [clojure.test :refer [deftest is testing]]))

(def species {1 {:taxonomy-id 1
                 :taxonomy-species "Wolf"
                 :taxonomy-genus "Smiley"
                 :taxonomy-label "Smiley Wolf"}
              2 {:taxonomy-id 2
                 :taxonomy-genus "Yellow"
                 :taxonomy-species "Spotted Cat"
                 :taxonomy-label "Yellow Spotted Cat"}})

(deftest nil-search-returns-all
  (let [expected [{:sightings []
                   :media-id 1}]
        results [{:sightings []
                  :media-id 1}]]
    (is (= (sut/only-matching nil species results) expected))))

(deftest nil-search-returns-all
  (let [expected [{:sightings []
                   :media-id 1}]
        results [{:sightings []
                  :media-id 1}]]
    (is (= (sut/only-matching nil species results) expected))))

(deftest empty-search-returns-all
  (let [expected [{:sightings []
                   :media-id 1}]
        results [{:sightings []
                  :media-id 1}]]
    (is (= (sut/only-matching "" species results) expected))))

(deftest species-search-returns-records-with-matching-species
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-id 2}]]
    (is (= (sut/only-matching "Smiley" species results) expected))))

(deftest species-search-is-case-insensitive
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-id 2}]]
    (is (= (sut/only-matching "smiley" species results) expected))))

(deftest species-search-matches-middle-of-string
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-id 2}]]
    (is (= (sut/only-matching "ley wolf" species results) expected))))

(deftest non-matching-species-are-ignored
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 2}]
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-id 2}]]
    (is (= (sut/only-matching "ley wolf" species results) expected))))

(deftest search-by-site
  (let [expected [{:sightings []
                   :site-name "MySite"
                   :media-id 1}]
        results [{:sightings []
                  :site-name "MySite"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "someothersite"
                  :media-id 2}]]
    (is (= (sut/only-matching "mysite" species results) expected))))

(deftest matches-any-field
  (let [expected [{:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                  {:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "wolf" species results) expected))))

(deftest terms-separated-by-space-are-conjunctive
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "wolf site" species results) expected))))

(deftest terms-separated-by-pipe-are-disjunctive
  (let [expected [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}
                 {:sightings [{:taxonomy-id 2}]
                  :site-name "Excluded"
                  :media-id 3}]]
    (is (= (sut/only-matching "wolf site|den" species results) expected))))

(deftest allows-field-to-be-specified
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "taxonomy-species:wolf" species results) expected))))

(deftest allows-field-shorthand-for-species
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "species:wolf" species results) expected))))

(deftest allows-field-shorthand-for-site
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "site:mysite" species results) expected))))

(deftest allows-field-shorthand-for-camera
  (let [expected [{:sightings []
                  :camera-name "ABC01"
                   :media-id 1}]
        results [{:sightings []
                  :camera-name "ABC01"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :camera-name "XYZ3"
                  :media-id 2}]]
    (is (= (sut/only-matching "camera:abc" species results) expected))))

(deftest ignores-nonsense-fields
  (let [results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching "nonexisty:wolf" species {}) '()))))

(deftest complex-searches-return-expected-results
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 2}]
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]]
    (is (= (sut/only-matching
            "taxonomy-species:wolf mysite|taxonomy-species:cat mysite"
            species results) expected))))

(deftest finds-attention-needed-true-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-attention-needed true
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 2}]
                  :media-attention-needed false
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-attention-needed true
                  :media-id 2}]]
    (is (= (sut/only-matching "flagged:true" species results) expected))))

(deftest finds-media-attention-needed-false-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-attention-needed false
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 1}]
                  :media-attention-needed false
                  :media-id 2}
                 {:sightings [{:taxonomy-id 2}]
                  :media-attention-needed true
                  :media-id 1}]]
    (is (= (sut/only-matching "flagged:false" species results) expected))))

(deftest finds-media-processed-true-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-processed true
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 2}]
                  :media-processed false
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :media-processed true
                  :media-id 2}]]
    (is (= (sut/only-matching "processed:true" species results) expected))))

(deftest finds-media-processed-false-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-processed false
                   :media-id 2}]
        results [{:sightings [{:taxonomy-id 1}]
                  :media-processed false
                  :media-id 2}
                 {:sightings [{:taxonomy-id 2}]
                  :media-processed true
                  :media-id 1}]]
    (is (= (sut/only-matching "processed:false" species results) expected))))

(deftest trap-station-ids-are-matched-exactly
  (let [expected [{:sightings [{:taxonomy-id 2}]
                   :trap-station-id 5
                   :media-id 1}]
        results [{:sightings [{:taxonomy-id 1}]
                  :trap-station-id 50
                  :media-id 2
                  :media-processed true}
                 {:sightings [{:taxonomy-id 2}]
                  :trap-station-id 5
                  :media-id 1}]]
    (is (= (sut/only-matching "trapid:5" species results) expected))))

(deftest exact-matches-are-respected
  (let [search "sighting-sex:f"
        expected [{:sightings [{:taxonomy-id 1 :sighting-sex "F"}]
                   :media-id 2
                   :media-processed true}]
        results [{:sightings [{:taxonomy-id 1 :sighting-sex "F"}]
                  :media-id 2
                  :media-processed true}
                 {:sightings [{:taxonomy-id 2 :sighting-sex "unidentified"}]
                  :media-id 1
                  :media-processed false}
                 {:sightings [{:taxonomy-id 3 :sighting-sex nil}]
                  :media-id 3
                  :media-processed false}]]
    (is (= (sut/only-matching search species results) expected))))

(deftest term-formatter-replaces-spaces
  (let [search "this is a test"]
    (is (= (sut/format-terms search)
           "this+++is+++a+++test"))))

(deftest term-formatter-leaves-spaces-in-quotes-alone
  (let [search "this is \"a test\""]
    (is (= (sut/format-terms search)
           "this+++is+++a test"))))
