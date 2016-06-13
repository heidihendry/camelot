(ns camelot.component.library-test
  (:require [camelot.component.library :as sut]
            [cljs.test :refer-macros [deftest is testing]]))

(def species {1 {:species-id 1
                 :species-scientific-name "Smiley Wolf"}
              2 {:species-id 2
                 :species-scientific-name "Yellow Spotted Cat"}})

(deftest nil-search-returns-all
  (let [results [{:sightings []
                  :media-id 1}]
        data {:search {:terms nil
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) results))))

(deftest empty-search-returns-all
  (let [results [{:sightings []
                  :media-id 1}]
        data {:search {:terms ""
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) results))))

(deftest species-search-returns-records-with-matching-species
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :media-id 2}]
        data {:search {:terms "Smiley"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest species-search-is-case-insensitive
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :media-id 2}]
        data {:search {:terms "smiley"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest species-search-matches-middle-of-string
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results [{:sightings []
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :media-id 2}]
        data {:search {:terms "ley wolf"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest non-matching-species-are-ignored
  (let [expected [{:sightings [{:species-id 1}]
                   :media-id 2}]
        results [{:sightings [{:species-id 2}]
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :media-id 2}]
        data {:search {:terms "ley wolf"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest search-by-site
  (let [expected [{:sightings []
                   :site-name "MySite"
                   :media-id 1}]
        results [{:sightings []
                  :site-name "MySite"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "someothersite"
                  :media-id 2}]
        data {:search {:terms "mysite"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest matches-any-field
  (let [results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        data {:search {:terms "wolf"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) results))))

(deftest terms-separated-by-space-are-conjunctive
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        data {:search {:terms "wolf site"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest terms-separated-by-pipe-are-disjunctive
  (let [expected [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}
                 {:sightings [{:species-id 2}]
                  :site-name "Excluded"
                  :media-id 3}]
        data {:search {:terms "wolf site|den"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest allows-field-to-be-specified
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        data {:search {:terms "species:wolf"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))

(deftest ignores-nonsense-fields
  (let [results [{:sightings []
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        data {:search {:terms "nonexisty:wolf"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) '()))))

(deftest complex-searches-return-expected-results
  (let [expected [{:sightings [{:species-id 1}]
                   :site-name "MySite"
                   :media-id 2}]
        results [{:sightings [{:species-id 2}]
                  :site-name "Wolf Den"
                  :media-id 1}
                 {:sightings [{:species-id 1}]
                  :site-name "MySite"
                  :media-id 2}]
        data {:search {:terms "species:wolf mysite|species:cat mysite"
                       :results results}
              :species species}]
    (is (= (sut/only-matching data) expected))))
