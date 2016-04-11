(ns camelot.processing.util-test
  (:require [midje.sweet :refer :all]
            [camelot.processing.util :refer :all]
            [camelot.processing.settings :refer [gen-state]]))

(facts "Path Descriptions"
  (fact "Metadata can be described with a keyword matching corresponding to its path"
    (let [config {:language :en}]
      (path-description (gen-state config) [:camera :make]) => "Camera Make"
      (path-description (gen-state config) [:datetime]) => "Date/Time")))
