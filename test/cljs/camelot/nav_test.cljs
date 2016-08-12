(ns camelot.nav-test
  (:require-macros [cljs.test :refer (is deftest testing)])
  (:require [cljs.test]
            [camelot.nav :as sut]))

(deftest nav-url-test
  (testing "Nav Up"
    (testing "Should be able to go up 1 level"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" 1)
             "http://myhost.com/path/to")))

    (testing "Should be able to go up 2 levels"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" 2)
             "http://myhost.com/path")))

    (testing "Should be able to go up 3 levels"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" 3)
             "http://myhost.com")))

    (testing "Should ignore trailing '/'s"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing/" 1)
             "http://myhost.com/path/to")))

    (testing "Should stop when researching hostname"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" 4)
             "http://myhost.com")))

    (testing "Should not change the URL if going up zero levels"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" 0)
             "http://myhost.com/path/to/thing")))

    (testing "Should not change the URL if going up negative levels"
      (is (= (sut/nav-up-url "http://myhost.com/path/to/thing" -1)
             "http://myhost.com/path/to/thing")))

    (testing "Should complain if arguments are not of the expected type"
      (is (thrown? js/Error (sut/nav-up-url -1 "http://myhost.com/path/to/thing"))))))
