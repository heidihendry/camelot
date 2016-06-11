(ns camelot.import.util-test
  (:require [camelot.import.util :refer :all]
            [camelot.application :as app]
            [midje.sweet :refer :all]))

(facts "Path Descriptions"
  (fact "Metadata can be described with a keyword matching corresponding to its path"
    (let [config {:language :en}]
      (path-description (app/gen-state config) [:camera :make]) => "Camera Make"
      (path-description (app/gen-state config) [:datetime]) => "Date/Time")))
