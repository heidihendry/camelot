(ns camelot.model.associated-taxonomy-test
  (:require [camelot.model.associated-taxonomy :as sut]
            [camelot.model.survey-taxonomy :as survey-taxonomy]
            [camelot.model.taxonomy :as taxonomy]
            [camelot.model.survey :as survey]
            [clojure.test :refer :all]
            [camelot.test-util.state :as state]
            [clojure.java.jdbc :as jdbc]))

(defn create!
  [data]
  (sut/create! (state/gen-state) (sut/tassociated-taxonomy data)))

(deftest test-create!
  (testing "create!"
    (testing "Associates all surveys if survey-id not given and mapping does not exist."
      (let [result (atom [])]
        (with-redefs [jdbc/db-transaction* (fn [db body & ks] (body {}))
                      survey-taxonomy/create! (fn [s data] (swap! result conj data))
                      survey-taxonomy/get-specific-by-relations (fn [& args] nil)
                      taxonomy/get-or-create! (fn [& args] {:taxonomy-id 1})
                      survey/get-all (fn [s] [{:survey-id 1} {:survey-id 2}])]
          (create! {:taxonomy-species "spp"})
          (is (= (map #(into {} %) @result)
                 [{:survey-id 1 :taxonomy-id 1}
                  {:survey-id 2 :taxonomy-id 1}])))))

    (testing "Associates only the given survey, should it be specified."
      (let [result (atom [])]
        (with-redefs [jdbc/db-transaction* (fn [db body & ks] (body {}))
                      survey-taxonomy/create! (fn [s data] (swap! result conj data))
                      survey-taxonomy/get-specific-by-relations (fn [& args] nil)
                      taxonomy/get-or-create! (fn [& args] {:taxonomy-id 1})
                      survey/get-all (fn [s] [{:survey-id 1} {:survey-id 2}])]
          (create! {:taxonomy-species "spp" :survey-id 1})
          (is (= (map #(into {} %) @result)
                 [{:survey-id 1 :taxonomy-id 1}])))))

    (testing "Does not create an association for given survey should one exist."
      (let [result (atom [])]
        (with-redefs [jdbc/db-transaction* (fn [db body & ks] (body {}))
                      survey-taxonomy/create! (fn [s data] (swap! result conj data))
                      survey-taxonomy/get-specific-by-relations (fn [& args] {})
                      taxonomy/get-or-create! (fn [& args] {:taxonomy-id 1})
                      survey/get-all (fn [s] [{:survey-id 1} {:survey-id 2}])]
          (create! {:taxonomy-species "spp" :survey-id 1})
          (is (= @result) []))))

    (testing "Does not create an association for any survey should all associations exist."
      (let [result (atom [])]
        (with-redefs [jdbc/db-transaction* (fn [db body & ks] (body {}))
                      survey-taxonomy/create! (fn [s data] (swap! result conj data))
                      survey-taxonomy/get-specific-by-relations (fn [& args] {})
                      taxonomy/get-or-create! (fn [& args] {:taxonomy-id 1})
                      survey/get-all (fn [s] [{:survey-id 1} {:survey-id 2}])]
          (create! {:taxonomy-species "spp"})
          (is (= @result) []))))))
