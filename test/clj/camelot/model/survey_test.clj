(ns camelot.model.survey-test
  "Tests around the survey model."
  (:require [camelot.model.survey :as sut]
            [camelot.test-util.state :as state]
            [clojure.test :refer :all]))

(deftest test-survey-settings
  (letfn [(survey-settings []
            (let [sighting-fields [{:survey-id 1 :data 1}
                                   {:survey-id 2 :data 2}]]
              (with-redefs [camelot.model.sighting-field/get-all
                            (constantly sighting-fields)

                            sut/get-all
                            (constantly [{:survey-id 1} {:survey-id 2}
                                         {:survey-id 3}])]
                (sut/survey-settings (state/gen-state)))))]

    (testing "Should return the expected survey settings"
      (is (= (survey-settings)
             {1 {:survey-id 1 :sighting-fields [{:survey-id 1 :data 1}]}
              2 {:survey-id 2 :sighting-fields [{:survey-id 2 :data 2}]}
              3 {:survey-id 3 :sighting-fields []}})))))
