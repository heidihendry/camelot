(ns camelot.state.datasets-test
  (:require [camelot.state.datasets :as sut]
            [camelot.testutil.mock :as mock]
            [clojure.test :as t]))

(defn- assoc-dataset-raw
  [datasets id]
  (.set-context datasets ::sut/dataset id))

(t/deftest test-reload
  (t/testing "reload"
    (t/testing "should return successfully if no dataset is in context"
      (let [datasets (mock/datasets {:default {}} :default)]
        (t/is (instance? (class datasets) (sut/reload! datasets)))))

    (t/testing "should return successfully if dataset in context is still available"
      (let [datasets (mock/datasets {:default {}} :default)]
        (t/is (instance? (class datasets) (sut/reload! (assoc-dataset-raw datasets :default))))))

    (t/testing "should throw if dataset is no longer available"
      (let [datasets (mock/datasets {:default {}} :default)]
        (t/is (thrown-with-msg? RuntimeException #"^Currently selected dataset was disconnected during reload$"
                                (sut/reload! (assoc-dataset-raw datasets :non-existent))))))))
