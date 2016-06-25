(ns camelot.util.filter-test
  (:require [camelot.util.filter :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

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
        results {1 {:sightings []
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching nil data) expected))))

(deftest empty-search-returns-all
  (let [expected [{:sightings []
                   :media-id 1}]
        results {1 {:sightings []
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "" data) expected))))

(deftest species-search-returns-records-with-matching-species
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "Smiley" data) expected))))

(deftest species-search-is-case-insensitive
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "smiley" data) expected))))

(deftest species-search-matches-middle-of-string
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "ley wolf" data) expected))))

(deftest non-matching-species-are-ignored
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 2}]
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "ley wolf" data) expected))))

(deftest search-by-site
  (let [expected [{:sightings []
                   :site-name "MySite"
                   :media-id 1}]
        results {1 {:sightings []
                    :site-name "MySite"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "someothersite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "mysite" data) expected))))

(deftest matches-any-field
  (let [expected [{:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                  {:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf" data) expected))))

(deftest terms-separated-by-space-are-conjunctive
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1  {:sightings []
                     :site-name "Wolf Den"
                     :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf site" data) expected))))

(deftest terms-separated-by-pipe-are-disjunctive
  (let [expected [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:taxonomy-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}
                 3 {:sightings [{:taxonomy-id 2}]
                    :site-name "Excluded"
                    :media-id 3}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf site|den" data) expected))))

(deftest allows-field-to-be-specified
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "taxonomy-species:wolf" data) expected))))

(deftest allows-field-shorthand-for-species
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "species:wolf" data) expected))))

(deftest allows-field-shorthand-for-site
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "site:mysite" data) expected))))

(deftest allows-field-shorthand-for-camera
  (let [expected [{:sightings []
                  :camera-name "ABC01"
                   :media-id 1}]
        results {1 {:sightings []
                    :camera-name "ABC01"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :camera-name "XYZ3"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "camera:abc" data) expected))))

(deftest ignores-nonsense-fields
  (let [results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "nonexisty:wolf" data) '()))))

(deftest complex-searches-return-expected-results
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 2}]
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching
            "taxonomy-species:wolf mysite|taxonomy-species:cat mysite"
            data) expected))))

(deftest finds-attention-needed-true-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-attention-needed true
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 2}]
                    :media-attention-needed false
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-attention-needed true
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "flag:true" data) expected))))

(deftest finds-media-attention-needed-false-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-attention-needed false
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 1}]
                    :media-attention-needed false
                    :media-id 2}
                 2 {:sightings [{:taxonomy-id 2}]
                    :media-attention-needed true
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "flag:false" data) expected))))

(deftest finds-media-processed-true-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-processed true
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 2}]
                    :media-processed false
                    :media-id 1}
                 2 {:sightings [{:taxonomy-id 1}]
                    :media-processed true
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "proc:true" data) expected))))

(deftest finds-media-processed-false-boolean
  (let [expected [{:sightings [{:taxonomy-id 1}]
                   :media-processed false
                   :media-id 2}]
        results {1 {:sightings [{:taxonomy-id 1}]
                    :media-processed false
                    :media-id 2}
                 2 {:sightings [{:taxonomy-id 2}]
                    :media-processed true
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "proc:false" data) expected))))

(deftest supports-unprocessed-only-flag
  (let [expected [{:sightings [{:taxonomy-id 2}]
                   :media-id 1
                   :media-processed false}]
        results {1 {:sightings [{:taxonomy-id 1}]
                    :media-id 2
                    :media-processed true}
                 2 {:sightings [{:taxonomy-id 2}]
                    :media-id 1
                    :media-processed false}}
        data {:search {:results results
                       :unprocessed-only true}
              :species species}]
    (is (= (sut/only-matching nil data) expected))))

(deftest trap-station-ids-are-matched-exactly
  (let [expected [{:sightings [{:taxonomy-id 2}]
                   :trap-station-id 5
                   :media-id 1}]
        results {1 {:sightings [{:taxonomy-id 1}]
                    :trap-station-id 50
                    :media-id 2
                    :media-processed true}
                 2 {:sightings [{:taxonomy-id 2}]
                    :trap-station-id 5
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "trapid:5" data) expected))))

(deftest unprocessed-only-flag-supports-disjunction
  (let [search "taxonomy-species:wolf mysite|taxonomy-species:cat|wolf"]
    (is (= (sut/append-subfilters search {:unprocessed-only true})
           (str "taxonomy-species:wolf mysite proc:false|"
                "taxonomy-species:cat proc:false|"
                "wolf proc:false")))))

(deftest flagged-only-supports-disjunction
  (let [search "taxonomy-species:wolf mysite|taxonomy-species:cat|wolf"]
    (is (= (sut/append-subfilters search {:flagged-only true})
           (str "taxonomy-species:wolf mysite flag:true|"
                "taxonomy-species:cat flag:true|"
                "wolf flag:true")))))

(deftest trap-id-supports-disjunction
  (let [search "taxonomy-species:wolf mysite|taxonomy-species:cat|wolf"]
    (is (= (sut/append-subfilters search {:trap-station-id 5})
           (str "taxonomy-species:wolf mysite trapid:5|"
                "taxonomy-species:cat trapid:5|"
                "wolf trapid:5")))))

(deftest subfilters-can-be-combined
  (let [search "taxonomy-species:wolf mysite|taxonomy-species:cat|wolf"]
    (is (= (sut/append-subfilters search {:unprocessed-only true
                                          :trap-station-id 5})
           (str "taxonomy-species:wolf mysite proc:false trapid:5|"
                "taxonomy-species:cat proc:false trapid:5|"
                "wolf proc:false trapid:5")))))