(ns camelot.report.module.loader-test
  (:require [camelot.report.module.loader :as sut]
            [camelot.util.file :as file]
            [clojure.java.io :as io]
            [camelot.testutil.state :as state]
            [clojure.test :refer :all]))

(def fake-files
  (map io/file ["/path/file1.clj"
                "/path/to/file2.clj"
                "/otherfile/file"
                "/somedir/"]))

(defn gen-state
  []
  (assoc-in (state/gen-state) [:config :paths :config] (io/file "/configpath/")))

(defmacro with-test-redefs
  [& body]
  `(with-redefs [file/exists? (constantly true)
                 file-seq (constantly fake-files)
                 file/readable? (constantly true)
                 file/file? (constantly true)
                 sut/load-module-file identity

                 file/mkdirs
                 #(throw (RuntimeException. "Should not call mkdirs"))]
     ~@body))

(deftest test-load-user-modules
  (testing "load-user-modules"
    (is (= (with-test-redefs (sut/load-user-modules (gen-state)))
           [(io/file "/path/file1.clj")
            (io/file "/path/to/file2.clj")]))))

(deftest test-module-path
  (testing "module-path"
    (is (= (sut/module-path (gen-state))
           (io/file "/configpath/modules")))))
