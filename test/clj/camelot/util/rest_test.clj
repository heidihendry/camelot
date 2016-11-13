(ns camelot.util.rest-test
  (:require
   [camelot.util.config :as config]
   [camelot.util.rest :as sut]
   [clojure.test :refer :all]))

(deftest test-list-available
  (testing "List available resources"
    (testing "Should parse the ID string"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] {:data id})]
          (is (= (:body (sut/list-available f "1" {})) {:data 1})))))

    (testing "Should not try to parse IDs which are already numeric"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] {:data id})]
          (is (= (:body (sut/list-available f 1 {})) {:data 1})))))))

(deftest test-list-resources
  (testing "List resources"
    (testing "Should generates URIs for the records"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c] [{:thing-id 1}
                         {:thing-id 2}])]
          (is (= (:body (sut/list-resources f :thing {})) [{:thing-id 1
                                                            :uri "/things/1"}
                                                           {:thing-id 2
                                                            :uri "/things/2"}])))))

    (testing "Should generate URIs for records given a parent ID."
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] (if (= id 3)
                             [{:thing-id 1}
                              {:thing-id 2}]))]
          (is (= (:body (sut/list-resources f :thing "3" {})) [{:thing-id 1
                                                                :uri "/things/1"}
                                                               {:thing-id 2
                                                                :uri "/things/2"}])))))

    (testing "Should not try to parse IDs which are already numeric."
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] (if (= id 3)
                             [{:thing-id 1}
                              {:thing-id 2}]))]
          (is (= (:body (sut/list-resources f :thing 3 {})) [{:thing-id 1
                                                              :uri "/things/1"}
                                                             {:thing-id 2
                                                              :uri "/things/2"}])))))))

(deftest test-specific-resource
  (testing "Specific resource"
    (testing "Should cursorise data"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] {:result :success})]
          (is (= (:body (sut/specific-resource f "30" {})) {:result {:value :success}})))))

    (testing "Should parse ID before calling `f'"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] (when (= id 30)
                             {:result :success}))]
          (is (= (:body (sut/specific-resource f "30" {})) {:result {:value :success}})))))

    (testing "Should not try to parse numeric IDs before calling `f'"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id] (when (= id 30)
                             {:result :success}))]
          (is (= (:body (sut/specific-resource f 30 {})) {:result {:value :success}})))))))

(deftest test-update-resource
  (testing "Update resource"
    (testing "Should Cursorise and decursorise data"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:somedata {:value "Hello World"}}
              f (fn [c id data]
                  (when (and (= (:somedata data) "Hello World") (= id 1))
                    data))]
          (is (= (:body (sut/update-resource f "1" test-data {})) {:somedata {:value "Hello World"}})))))

    (testing "Should parse IDs"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:parent-id {:value "100"}}
              f (fn [c id data]
                  (when (and (= (:parent-id data) 100) (= id 150))
                    data))]
          (is (= (:body (sut/update-resource f "150" test-data {})) {:parent-id {:value 100}})))))

    (testing "Should not parse IDs which are already numeric"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:parent-id {:value 100}}
              f (fn [c id data]
                  (when (and (= (:parent-id data) 100) (= id 150))
                    data))]
          (is (= (:body (sut/update-resource f 150 test-data {})) {:parent-id {:value 100}})))))

    (testing "Should parse floating-point fields"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:trap-station-longitude {:value "30.5"}
                         :trap-station-latitude {:value "-5.95"}}
              f (fn [c id data] data)]
          (is (= (:body (sut/update-resource f "150" test-data {})) {:trap-station-longitude
                                                                     {:value 30.5}
                                                                     :trap-station-latitude
                                                                     {:value -5.95}})))))

    (testing "Should not try to parse floating-point fields which are already numeric"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:trap-station-longitude {:value 30.5}
                         :trap-station-latitude {:value -5.95}}
              f (fn [c id data] data)]
          (is (= (:body (sut/update-resource f "150" test-data {})) {:trap-station-longitude
                                                                     {:value 30.5}
                                                                     :trap-station-latitude
                                                                     {:value -5.95}})))))))

(deftest test-create-resource
  (testing "Create resource"
    (testing "Should cursorise and decursorise data"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:somedata {:value "Hello World"}}
              f (fn [c data]
                  (when (= (:somedata data) "Hello World")
                    data))]
          (is (= (:body (sut/create-resource f test-data {})) {:somedata {:value "Hello World"}})))))

    (testing "Should parse IDs"
      (with-redefs [config/config (fn [s] s)]
        (let [test-data {:parent-id {:value "100"}}
              f (fn [c data]
                  (when (= (:parent-id data) 100)
                    data))]
          (is (= (:body (sut/create-resource f test-data {})) {:parent-id {:value 100}})))))))

(deftest test-delete-resource
  (testing "Delete resource"
    (testing "Should call correctly and guarantee valid response"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id]
                  (when (= id 30)
                    {:result :success}))]
          (is (= (:body (sut/delete-resource f "30" {})) {:data {:result :success}})))))

    (testing "Should not try to parse IDs which are already numeric"
      (with-redefs [config/config (fn [s] s)]
        (let [f (fn [c id]
                  (when (= id 30)
                    {:result :success}))]
          (is (= (:body (sut/delete-resource f 30 {})) {:data {:result :success}})))))))
