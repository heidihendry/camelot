(ns camelot.nav-util-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test]
            [camelot.nav-util :as sut]))

(deftest nav-url-test
  (testing "Nav Up"
    (testing "Should be able to go up 1 level"
      (is (= (sut/nav-up-url "/#/path/to/thing" 1)
             "/#/path/to")))

    (testing "Should be able to go up 2 levels"
      (is (= (sut/nav-up-url "/#/path/to/thing" 2)
             "/#/path")))

    (testing "Going up to the top level should fall back to the homepage."
      (is (= (sut/nav-up-url "/#/path/to/thing" 3)
             "/#/organisation")))

    (testing "Should ignore trailing '/'s"
      (is (= (sut/nav-up-url "/#/path/to/thing/" 1)
             "/#/path/to")))

    (testing "Should fall back to the homepage when out of pages"
      (is (= (sut/nav-up-url "/#/path/to/thing" 4)
             "/#/organisation")))

    (testing "Should not change the URL if going up zero levels"
      (is (= (sut/nav-up-url "/#/path/to/thing" 0)
             "/#/path/to/thing")))

    (testing "Should not change the URL if going up negative levels"
      (is (= (sut/nav-up-url "/#/path/to/thing" -1)
             "/#/path/to/thing")))

    (testing "Should complain if arguments are not of the expected type"
      (is (thrown? js/Error (sut/nav-up-url -1 "/#/path/to/thing"))))))
