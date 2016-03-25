(ns ctdp.action.rename-photo-test
  (:require [clojure.test :refer :all]
            [ctdp.album :refer :all]
            [ctdp.config :refer [gen-state]]
            [clj-time.core :as t]
            [schema.test :as st]
            [taoensso.tower :as tower]
            [ctdp.translations.core :refer :all]
            [ctdp.action.rename-photo :refer :all]))

(use-fixtures :once st/validate-schemas)

(deftest field-data-extraction-test
  (testing "Path lookup and extraction is successful"
    (let [paths [[:camera :make]
                 [:camera :model]]
          config {:rename {:fields paths} :language :en}
          metadata {:camera {:make "CameraMaker"
                             :model "CamModel"}
                    :camera-settings {:iso 9000}
                    :datetime (t/date-time 2015 01 01)}]
          (is (= ["CameraMaker" "CamModel"]
                 (extract-all-fields (gen-state config) metadata)))))

  (testing "Various data types are serialised to string"
    (let [paths [[:camera :make]
                 [:camera-settings :iso]
                 [:datetime]]
          config {:rename {:fields paths
                           :date-format "YYYY/MM/dd"}
                  :language :en}
          metadata {:camera {:make "CameraMaker"
                             :model "CamModel"}
                    :camera-settings {:iso 9000}
                    :datetime (t/date-time 2015 01 01)}]
      (is (= ["CameraMaker" "9000" "2015/01/01"]
             (extract-all-fields (gen-state config) metadata)))))

  (testing "Lookup should fail gracefully-ish"
    (let [paths [[:camTYPOera :make]
                 [:camera-settings :TYPOiso]
                 [:datetime]]
          config {:rename {:fields paths
                           :date-format "YYYY/MM/dd"}
                  :language :en}
          metadata {:camera {:make "CameraMaker"
                             :model "CamModel"}
                    :camera-settings {:iso 9000}
                    :datetime (t/date-time 2015 01 01)}]
      (is (thrown-with-msg? IllegalStateException
                            #"\[:camTYPOera :make\].*\[:camera-settings :TYPOiso\]"
                            (extract-all-fields (gen-state config)
                                                metadata)))))

  (testing "Files should be renamed in the album data-structure"
    (let [album {"file1" {:data "this was file1"}
                 "file2" {:data "this was file2"}
                 "file3" {:data "this was file3"}}
          renames [["file1" "file-renamed1"]
                   ["file2" "file-renamed2"]
                   ["file3" "file-renamed3"]]
          expected {"file-renamed1" {:data "this was file1"}
                    "file-renamed2" {:data "this was file2"}
                    "file-renamed3" {:data "this was file3"}}]
      (is (= (apply-renames album renames) expected)))))
