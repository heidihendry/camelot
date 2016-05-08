(ns camelot.util.rest-test
  (:require [camelot.util.rest :as sut]
            [midje.sweet :refer :all]))

(facts "List available resources"
  (fact "Should parse the ID string"
    (let [f (fn [c id] {:data id})]
      (:body (sut/list-available f "1")) => {:data 1}))

  (fact "Should not try to parse IDs which are already numeric"
    (let [f (fn [c id] {:data id})]
      (:body (sut/list-available f 1)) => {:data 1})))

(facts "List resources"
  (fact "Should generates URIs for the records"
    (let [f (fn [c] [{:thing-id 1}
                     {:thing-id 2}])]
      (:body (sut/list-resources f :thing)) => [{:thing-id 1
                                                 :uri "/things/1"}
                                                {:thing-id 2
                                                 :uri "/things/2"}]))

  (fact "Should generate URIs for records given a parent ID."
    (let [f (fn [c id] (if (= id 3)
                         [{:thing-id 1}
                          {:thing-id 2}]))]
      (:body (sut/list-resources f :thing "3")) => [{:thing-id 1
                                                     :uri "/things/1"}
                                                    {:thing-id 2
                                                     :uri "/things/2"}]))

  (fact "Should not try to parse IDs which are already numeric."
    (let [f (fn [c id] (if (= id 3)
                         [{:thing-id 1}
                          {:thing-id 2}]))]
      (:body (sut/list-resources f :thing 3)) => [{:thing-id 1
                                                     :uri "/things/1"}
                                                    {:thing-id 2
                                                     :uri "/things/2"}])))

(facts "Specific resource"
  (fact "Should cursorise data"
    (let [f (fn [c id] {:result :success})]
      (:body (sut/specific-resource f "30")) => {:result {:value :success}}))

  (fact "Should parse ID before calling `f'"
    (let [f (fn [c id] (when (= id 30)
                         {:result :success}))]
      (:body (sut/specific-resource f "30")) => {:result {:value :success}}))

  (fact "Should not try to parse numeric IDs before calling `f'"
    (let [f (fn [c id] (when (= id 30)
                         {:result :success}))]
      (:body (sut/specific-resource f 30)) => {:result {:value :success}})))

(facts "Update resource"
  (fact "Should Cursorise and decursorise data"
    (let [test-data {:somedata {:value "Hello World"}}
          f (fn [c id data]
              (when (and (= (:somedata data) "Hello World") (= id 1))
                data))]
      (:body (sut/update-resource f "1" test-data)) => {:somedata {:value "Hello World"}}))

  (fact "Should parse IDs"
    (let [test-data {:parent-id {:value "100"}}
          f (fn [c id data]
              (when (and (= (:parent-id data) 100) (= id 150))
                data))]
      (:body (sut/update-resource f "150" test-data)) => {:parent-id {:value 100}}))

  (fact "Should not parse IDs which are already numeric"
      (let [test-data {:parent-id {:value 100}}
          f (fn [c id data]
              (when (and (= (:parent-id data) 100) (= id 150))
                data))]
        (:body (sut/update-resource f 150 test-data)) => {:parent-id {:value 100}}))

  (fact "Should parse floating-point fields"
    (let [test-data {:trap-station-longitude {:value "30.5"}
                     :trap-station-latitude {:value "-5.95"}}
          f (fn [c id data] data)]
      (:body (sut/update-resource f "150" test-data)) => {:trap-station-longitude
                                                          {:value 30.5}
                                                          :trap-station-latitude
                                                          {:value -5.95}}))

  (fact "Should not try to parse floating-point fields which are already numeric"
    (let [test-data {:trap-station-longitude {:value 30.5}
                     :trap-station-latitude {:value -5.95}}
          f (fn [c id data] data)]
      (:body (sut/update-resource f "150" test-data)) => {:trap-station-longitude
                                                          {:value 30.5}
                                                          :trap-station-latitude
                                                          {:value -5.95}})))

(facts "Create resource"
  (fact "Should cursorise and decursorise data"
    (let [test-data {:somedata {:value "Hello World"}}
          f (fn [c data]
              (when (= (:somedata data) "Hello World")
                data))]
      (:body (sut/create-resource f test-data)) => {:somedata {:value "Hello World"}}))

  (fact "Should parse IDs"
    (let [test-data {:parent-id {:value "100"}}
          f (fn [c data]
              (when (= (:parent-id data) 100)
                data))]
      (:body (sut/create-resource f test-data)) => {:parent-id {:value 100}})))

(facts "Delete resource"
  (fact "Should call correctly and guarantee valid response"
    (let [f (fn [c id]
              (when (= id 30)
                {:result :success}))]
      (:body (sut/delete-resource f "30")) => {:data {:result :success}}))

  (fact "Should not try to parse IDs which are already numeric"
    (let [f (fn [c id]
              (when (= id 30)
                {:result :success}))]
      (:body (sut/delete-resource f 30)) => {:data {:result :success}})))
