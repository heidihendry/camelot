(ns camelot.system.http.dataset-test
  (:require
   [camelot.system.protocols :as protocols]
   [camelot.testutil.mock :refer [defmock with-spies datasets]]
   [camelot.state.datasets :as datasets]
   [camelot.system.http.dataset :as sut]
   [clojure.test :as t]))

(defn- handler-context
  [calls handler]
  (datasets/get-dataset-context (get-in (ffirst (get calls handler)) [:system :datasets])))

(t/deftest test-wrap-dataset-selection
  (t/testing "wrap-dataset-selection"
    (t/testing "requests"
      (t/testing "context should be set based on requested dataset"
        (with-spies [calls]
          (let [handler (defmock [r] nil)
                request {:session {:dataset-id :special}
                         :system {:datasets (datasets {:default {} :special {}})}}]
            ((sut/wrap-dataset-selection handler) request)
            (t/is (= :special (handler-context (calls) handler))))))

      (t/testing "should have a default dataset-id injected if not set"
        (with-spies [calls]
          (let [handler (defmock [r] nil)
                request {:system {:datasets (datasets {:default {}})}}]
            ((sut/wrap-dataset-selection handler) request)
            (let [expected (assoc-in request [:session :dataset-id] :default)]
              (t/is (= :default (handler-context (calls) handler))))))))

    (t/testing "responses"
      (t/testing "should be left alone if a dataset-id is set in the request"
        (let [handler (constantly {})
              request {:session {:dataset-id :special}
                       :system {:datasets (datasets {:default {} :special {}})}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {} response))))

      (t/testing "should be have a default dataset-id set if one was not set in the request "
        (let [handler (constantly {})
              request {:system {:datasets (datasets {:default {}})}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :default}} response))))

      (t/testing "should not override a dataset-id set in the response"
        (let [handler (constantly {:session {:dataset-id :special}})
              request {:system {:datasets (datasets {:default {}})}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :special}} response))))

      (t/testing "should merge with other session values set in the response"
        (let [handler (constantly {:session {:some-key :some-value}})
              request {:system {:datasets (datasets {:default {}})}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :default
                              :some-key :some-value}}
                   response)))))))
