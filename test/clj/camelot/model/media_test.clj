(ns camelot.model.media-test
  (:require [camelot.model.media :as sut]
            [clojure.test :refer :all]
            [camelot.util.file :as file]
            [camelot.testutil.state :as state]
            [clojure.java.io :as io]))

(defn gen-state
  []
  (assoc (state/gen-state) :config {:path {:media "/mymedia/"}}))

(defn path-equal
  [file expect]
  (= file (io/file expect)))

(deftest test-path-to-file
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

(deftest test-delete-files!
  (testing "should delete expected files"
    (with-redefs [file/delete identity]
      (let [expected (map io/file ["/mymedia/fi/file1.png" "/mymedia/fi/thumb-file1.png"
                                   "/mymedia/fi/file2.jpg" "/mymedia/fi/thumb-file2.png"])]
        (is (= (sut/delete-files! (gen-state) ["file1.png" "file2.jpg"])
               expected))))))
