(ns camelot.component.library-test
  (:require [camelot.component.library :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

(def species {1 {:species-id 1
                 :species-scientific-name "Smiley Wolf"}
              2 {:species-id 2
                 :species-scientific-name "Yellow Spotted Cat"}})

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
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "Smiley" data) expected))))

(deftest species-search-is-case-insensitive
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "smiley" data) expected))))

(deftest species-search-matches-middle-of-string
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results {1 {:sightings []
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "ley wolf" data) expected))))

(deftest non-matching-species-are-ignored
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results {1 {:sightings [{:species-id 2}]
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
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
                 2 {:sightings [{:species-id 1}]
                    :site-name "someothersite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "mysite" data) expected))))

(deftest matches-any-field
  (let [expected [{:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                  {:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf" data) expected))))

(deftest terms-separated-by-space-are-conjunctive
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1  {:sightings []
                     :site-name "Wolf Den"
                     :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf site" data) expected))))

(deftest terms-separated-by-pipe-are-disjunctive
  (let [expected [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}
                 3 {:sightings [{:species-id 2}]
                    :site-name "Excluded"
                    :media-id 3}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "wolf site|den" data) expected))))

(deftest allows-field-to-be-specified
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "species-scientific-name:wolf" data) expected))))

(deftest allows-field-shorthand-for-species
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "species:wolf" data) expected))))

(deftest allows-field-shorthand-for-site
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
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
                 2 {:sightings [{:species-id 1}]
                    :camera-name "XYZ3"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "camera:abc" data) expected))))

(deftest ignores-nonsense-fields
  (let [results {1 {:sightings []
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "nonexisty:wolf" data) '()))))

(deftest complex-searches-return-expected-results
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results {1 {:sightings [{:species-id 2}]
                    :site-name "Wolf Den"
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :site-name "MySite"
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching
            "species-scientific-name:wolf mysite|species-scientific-name:cat mysite"
            data) expected))))

(deftest finds-attention-needed-true-boolean
  (let [expected [{:sightings [{:species-id 1}]
                   :media-attention-needed true
                   :media-id 2}]
        results {1 {:sightings [{:species-id 2}]
                    :media-attention-needed false
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :media-attention-needed true
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "attn:true" data) expected))))

(deftest finds-media-attention-needed-false-boolean
  (let [expected [{:sightings [{:species-id 1}]
                   :media-attention-needed false
                   :media-id 2}]
        results {1 {:sightings [{:species-id 1}]
                    :media-attention-needed false
                    :media-id 2}
                 2 {:sightings [{:species-id 2}]
                    :media-attention-needed true
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "attn:false" data) expected))))

(deftest finds-media-processed-true-boolean
  (let [expected [{:sightings [{:species-id 1}]
                   :media-processed true
                   :media-id 2}]
        results {1 {:sightings [{:species-id 2}]
                    :media-processed false
                    :media-id 1}
                 2 {:sightings [{:species-id 1}]
                    :media-processed true
                    :media-id 2}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "proc:true" data) expected))))

(deftest finds-media-processed-false-boolean
  (let [expected [{:sightings [{:species-id 1}]
                   :media-processed false
                   :media-id 2}]
        results {1 {:sightings [{:species-id 1}]
                    :media-processed false
                    :media-id 2}
                 2 {:sightings [{:species-id 2}]
                    :media-processed true
                    :media-id 1}}
        data {:search {:results results}
              :species species}]
    (is (= (sut/only-matching "proc:false" data) expected))))
