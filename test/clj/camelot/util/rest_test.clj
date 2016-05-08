(ns camelot.util.rest-test
  (:require [camelot.util.rest :as sut]
            [midje.sweet :refer :all]))

(facts "List available resources"
  (fact "Parses the ID string"
    (let [f (fn [c id] {:data id})]
      (:body (sut/list-available f "1")) => {:data 1})))

(facts "List resources"
  (fact "Generates URIs for the records"
    (let [f (fn [c] [{:thing-id 1}
                     {:thing-id 2}])]
      (:body (sut/list-resources f :thing)) => [{:thing-id 1
                                                 :uri "/things/1"}
                                                {:thing-id 2
                                                 :uri "/things/2"}]))

  (fact "Generates URIs for records given a parent ID."
    (let [f (fn [c id] (if (= id 3)
                         [{:thing-id 1}
                          {:thing-id 2}]))]
      (:body (sut/list-resources f :thing "3")) => [{:thing-id 1
                                                     :uri "/things/1"}
                                                    {:thing-id 2
                                                     :uri "/things/2"}])))

(facts "Update resource"
  (fact "Cursorises and decursorises data"
    (let [test-data {:somedata {:value "Hello World"}}
          f (fn [c id data]
              (when (and (= (:somedata data) "Hello World") (= id 1))
                data))]
      (:body (sut/update-resource f "1" test-data)) => {:somedata {:value "Hello World"}}))

  (fact "Parses IDs"
    (let [test-data {:parent-id {:value "100"}}
          f (fn [c id data]
              (when (and (= (:parent-id data) 100) (= id 150))
                data))]
      (:body (sut/update-resource f "150" test-data)) => {:parent-id {:value 100}})))

(facts "Create resource"
  (fact "Cursorises and decursorises data"
    (let [test-data {:somedata {:value "Hello World"}}
          f (fn [c data]
              (when (= (:somedata data) "Hello World")
                data))]
      (:body (sut/create-resource f test-data)) => {:somedata {:value "Hello World"}}))

  (fact "Parses IDs"
    (let [test-data {:parent-id {:value "100"}}
          f (fn [c data]
              (when (= (:parent-id data) 100)
                data))]
      (:body (sut/create-resource f test-data)) => {:parent-id {:value 100}})))

(facts "Delete resource"
  (fact "Calls correctly and guarantees valid response"
    (let [f (fn [c id]
              (when (= id 30)
                {:result :success}))]
      (:body (sut/delete-resource f "30")) => {:data {:result :success}})))
