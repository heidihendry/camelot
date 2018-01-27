(ns camelot.report.sighting-independence-test
  (:require
   [camelot.report.sighting-independence :as sut]
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [schema.test :as st]
   [camelot.testutil.state :as state]))

(defn gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(use-fixtures :once st/validate-schemas)

(defn ->record
  [data]
  (merge {:survey-id 1} data))

(defn extract-indep-sightings
  ([state survey-config sightings]
   (let [s (assoc state :survey-settings survey-config)]
     (sut/extract-independent-sightings s sightings)))
  ([state sightings]
   (extract-indep-sightings
    state
    {1 {:survey-sighting-independence-threshold 20
        :sighting-fields []}
     2 {:survey-sighting-independence-threshold 10
        :sighting-fields []}}
    sightings)))

(defn indep-sightings
  ([state survey-config sightings]
   (let [s (assoc state :survey-settings survey-config)]
     (sut/->independent-sightings s sightings)))
  ([state sightings]
   (indep-sightings
    state
    {1 {:survey-sighting-independence-threshold 20
        :sighting-fields []}}
    sightings)))

(def config
  {:infrared-iso-value-threshold 999
   :night-end-hour 5
   :night-start-hour 21
   :erroneous-infrared-threshold 0.5
   :sighting-independence-minutes-threshold 20})

(deftest test-extract-independent-sightings
  (testing "Sighting Independence extraction"
    (testing "A single sighting is extracted"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 1})))))

    (testing "Multiple species are extracted if present"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 2})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 1}
                                                           {:species-id 2
                                                            :count 2})))))

    (testing "Sightings are only considered independent if having sufficient temporal distance"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 3})))))

    (testing "A sighting exactly on the threshold is independent"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 20 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 40 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 4})))))

    (testing "A sighting may later need to be updated with a higher quantity"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 4})))))

    (testing "A single sighting may contain multiple species"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 5})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 4}
                                                           {:species-id 2
                                                            :count 7})))))

    (testing "Results are correct regardless of ordering of input"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                                  :taxonomy-id 1
                                  :taxonomy-genus "Yellow"
                                  :taxonomy-species "Spotted Housecat"
                                  :sighting-quantity 2})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 10 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 5})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 07 00 00)
                                  :taxonomy-id 2
                                  :taxonomy-genus "Smiley"
                                  :taxonomy-species "Wolf"
                                  :sighting-quantity 1})]
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state sightings) '({:species-id 1
                                                            :count 4}
                                                           {:species-id 2
                                                            :count 7}))))))


  (testing "Sighting fields"
    (testing "Should not respect value differences if affect-independence is false"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-inconsequential 1
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-inconsequential 2
                                  :sighting-quantity 1})]
            survey-configuration {10 {:survey-sighting-independence-threshold 10
                                      :sighting-fields
                                      [{:sighting-field-affects-independence false
                                        :sighting-field-key "inconsequential"}]}}
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state survey-configuration sightings)
               '({:species-id 1 :count 1})))))

    (testing "Should respect value differences if affect-independence is true"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-consequential 1
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-consequential 2
                                  :sighting-quantity 1})]
            survey-configuration {10 {:survey-sighting-independence-threshold 10
                                      :sighting-fields
                                      [{:sighting-field-affects-independence true
                                        :sighting-field-key "consequential"}]}}
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state survey-configuration sightings)
               '({:species-id 1 :count 2})))))

    (testing "Should take any difference is sighting fields as triggering independence"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom 1
                                  :field-special 2
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom 2
                                  :field-special 2
                                  :sighting-quantity 1})]
            survey-configuration {10 {:survey-sighting-independence-threshold 10
                                      :sighting-fields
                                      [{:sighting-field-affects-independence true
                                        :sighting-field-key "custom"}
                                       {:sighting-field-affects-independence true
                                        :sighting-field-key "special"}]}}
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state survey-configuration sightings)
               '({:species-id 1 :count 2})))))

    (testing "Should treat nil as a non-specific value and determine dependence"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom nil
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom 2
                                  :sighting-quantity 1})]
            survey-configuration {10 {:survey-sighting-independence-threshold 10
                                      :sighting-fields
                                      [{:sighting-field-affects-independence true
                                        :sighting-field-key "custom"}]}}
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state survey-configuration sightings)
               '({:species-id 1 :count 1})))))

    (testing "Should treat empty string as a non-specific value and determine dependence"
      (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 01 01 06 00 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom ""
                                  :sighting-quantity 1})
                       (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                  :taxonomy-id 1
                                  :survey-id 10
                                  :field-custom "Simba"
                                  :sighting-quantity 1})]
            survey-configuration {10 {:survey-sighting-independence-threshold 10
                                      :sighting-fields
                                      [{:sighting-field-affects-independence true
                                        :sighting-field-key "custom"}]}}
            state (gen-state-helper config)]
        (is (= (extract-indep-sightings state survey-configuration sightings)
               '({:species-id 1 :count 1}))))))

  (testing "Copes with species ID without a sighting quantity"
    (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                :taxonomy-id 3
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                :taxonomy-id 1
                                :taxonomy-genus "Yellow"
                                :taxonomy-species "Spotted Housecat"
                                :sighting-quantity 1})]
          state (gen-state-helper config)]
      (is (= (extract-indep-sightings state sightings)
             '({:species-id 1 :count 1})))))

  (testing "Copes with sighting quantity without a taxonomy ID"
    (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                :sighting-quantity 1
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 01 01 06 05 00)
                                :taxonomy-id 1
                                :taxonomy-genus "Yellow"
                                :taxonomy-species "Spotted Housecat"
                                :sighting-quantity 1})]
          state (gen-state-helper config)]
      (is (= (extract-indep-sightings state sightings)
             '({:species-id 1 :count 1})))))

  (testing "May use a different independence threshold for different surveys"
    (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                :sighting-quantity 1
                                :survey-id 1
                                :taxonomy-id 1
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 25 00)
                                :sighting-quantity 1
                                :survey-id 1
                                :taxonomy-id 1
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                :sighting-quantity 1
                                :survey-id 2
                                :trap-station-session-id 2
                                :taxonomy-id 2
                                :media-id 2})
                     (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 25 00)
                                :sighting-quantity 1
                                :survey-id 2
                                :trap-station-session-id 2
                                :taxonomy-id 2
                                :media-id 2})]
          state (gen-state-helper config)]
      (is (= (extract-indep-sightings state sightings)
             '({:species-id 1 :count 1}
               {:species-id 2 :count 2})))))

  (testing "Should use global threshold configuration if survey configuration not found"
    (let [sightings [(->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                :sighting-quantity 1
                                :survey-id -1
                                :taxonomy-id 1
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 25 00)
                                :sighting-quantity 1
                                :survey-id -1
                                :taxonomy-id 1
                                :trap-station-session-id 1
                                :media-id 1})
                     (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 31 00)
                                :sighting-quantity 3
                                :survey-id -1
                                :taxonomy-id 1
                                :trap-station-session-id 1
                                :media-id 1})]
          state (gen-state-helper config)]
      (is (= (extract-indep-sightings state sightings)
             '({:species-id 1 :count 4}))))))

(deftest test-->independent-sightings
  (testing "Sighting independence"
    (testing "Records without sightings are excluded"
      (let [record (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                              :trap-station-session-id 1
                              :taxonomy-id nil
                              :sighting-quantity nil
                              :media-id 1})
            state (gen-state-helper config)]
        (is (= (indep-sightings state record) []))))

    (testing "Copes with species ID without a sighting quantity"
      (let [record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                    :taxonomy-id 1
                    :trap-station-session-id 1
                    :media-id 1}
            state (gen-state-helper config)]
        (is (= (indep-sightings state record) []))))

    (testing "Records with sightings are included and sorted by date"
      (let [records (list (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                                     :taxonomy-id 1
                                     :sighting-quantity 1
                                     :media-id 1})
                          (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 15 00)
                                     :taxonomy-id 1
                                     :sighting-quantity 3
                                     :media-id 2})
                          (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 9 00)
                                     :taxonomy-id 2
                                     :sighting-quantity 2
                                     :media-id 3}))
            state (gen-state-helper config)]
        (is (= (indep-sightings state records)
               [(->record {:media-capture-timestamp (t/date-time 2015 1 1 7 9 00)
                           :sighting-independence-window-end (t/date-time 2015 1 1 7 29 00)
                           :taxonomy-id 2
                           :sighting-quantity 2
                           :media-id 3})
                (->record {:media-capture-timestamp (t/date-time 2015 1 1 7 10 00)
                           :sighting-independence-window-end (t/date-time 2015 1 1 7 30 00)
                           :taxonomy-id 1
                           :sighting-quantity 3
                           :media-id 1})]))))))
