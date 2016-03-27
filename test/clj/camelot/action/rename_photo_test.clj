(ns camelot.action.rename-photo-test
  (:require [midje.sweet :refer :all]
            [camelot.album :refer :all]
            [camelot.config :refer [gen-state]]
            [clj-time.core :as t]
            [schema.test :as st]
            [taoensso.tower :as tower]
            [camelot.translations.core :refer :all]
            [camelot.action.rename-photo :refer :all]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "field data extraction"
  (fact "Path lookup and extraction is successful"
    (let [paths [[:camera :make]
                 [:camera :model]]
          config {:rename {:fields paths} :language :en}
          metadata {:camera {:make "CameraMaker"
                             :model "CamModel"}
                    :camera-settings {:iso 9000}
                    :datetime (t/date-time 2015 01 01)}]
      (extract-all-fields (gen-state config) metadata) => ["CameraMaker" "CamModel"]))

  (fact "Various data types are serialised to string"
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
      (extract-all-fields (gen-state config) metadata) => ["CameraMaker" "9000" "2015/01/01"]))

  (fact "Lookup should fail gracefully-ish"
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
      (extract-all-fields (gen-state config) metadata) => (throws IllegalStateException #"\[:camTYPOera :make\].*\[:camera-settings :TYPOiso\]")))

  (fact "Files should be renamed in the album data-structure"
    (let [album {"file1" {:data "this was file1"}
                 "file2" {:data "this was file2"}
                 "file3" {:data "this was file3"}}
          renames [["file1" "file-renamed1"]
                   ["file2" "file-renamed2"]
                   ["file3" "file-renamed3"]]
          expected {"file-renamed1" {:data "this was file1"}
                    "file-renamed2" {:data "this was file2"}
                    "file-renamed3" {:data "this was file3"}}]
      (apply-renames album renames) => expected)))
