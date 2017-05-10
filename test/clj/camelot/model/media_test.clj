(ns camelot.model.media-test
  (:require [camelot.model.media :as sut]
            [clojure.test :refer :all]
            [camelot.test-util.state :as state]
            [clojure.java.io :as io]))

(defn gen-state
  []
  (assoc (state/gen-state) :config {:path {:media "/mymedia/"}}))

(defn path-equal
  [file expect]
  (= file (io/file expect)))

(deftest path-to-file-test
  (testing "Should return expected paths for image variants image"
    (are [variant result]
        (path-equal (sut/path-to-file (gen-state) variant "abc" "jpg") result)
      :original "/mymedia/ab/abc.jpg"
      :thumb "/mymedia/ab/thumb-abc.png"))

  (testing "Should respect filename path for image variants image"
    (is (path-equal (sut/path-to-file (gen-state) :original "xyz123" "jpg")
                    "/mymedia/xy/xyz123.jpg")))

  (testing "Should respect image format"
    (is (path-equal (sut/path-to-file (gen-state) :original "abc" "png")
                    "/mymedia/ab/abc.png"))))
