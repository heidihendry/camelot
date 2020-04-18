(ns camelot.system.http.dataset-test
  (:require
   [camelot.testutil.mock :refer [defmock with-spies]]
   [camelot.system.http.dataset :as sut]
   [clojure.test :as t]))

(t/deftest test-wrap-dataset-selection
  (t/testing "wrap-dataset-selection"
    (t/testing "requests"
      (t/testing "should be left alone if dataset-id already set"
        (with-spies [calls]
          (let [handler (defmock [r] nil)
                request {:session {:dataset-id :special}
                         :system {:config {:dataset-ids [:default :special]}}}]
            ((sut/wrap-dataset-selection handler) request)
            (t/is (= [request] (first (get (calls) handler)))))))

      (t/testing "should have a default dataset-id injected if not set"
        (with-spies [calls]
          (let [handler (defmock [r] nil)
                request {:system {:config {:dataset-ids [:default]}}}]
            ((sut/wrap-dataset-selection handler) request)
            (let [expected (assoc-in request [:session :dataset-id] :default)]
              (t/is (= [expected] (first (get (calls) handler)))))))))

    (t/testing "responses"
      (t/testing "should be left alone if a dataset-id is set in the request"
        (let [handler (constantly {})
              request {:session {:dataset-id :special}
                       :system {:config {:dataset-ids [:default :special]}}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {} response))))

      (t/testing "should be have a default dataset-id set if one was not set in the request "
        (let [handler (constantly {})
              request {:system {:config {:dataset-ids [:default]}}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :default}} response))))

      (t/testing "should not override a dataset-id set in the response"
        (let [handler (constantly {:session {:dataset-id :special}})
              request {:system {:config {:dataset-ids [:default]}}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :special}} response))))

      (t/testing "should merge with other session values set in the response"
        (let [handler (constantly {:session {:some-key :some-value}})
              request {:system {:config {:dataset-ids [:default]}}}
              response ((sut/wrap-dataset-selection handler) request)]
          (t/is (= {:session {:dataset-id :default
                              :some-key :some-value}}
                   response)))))))
