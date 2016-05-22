(ns camelot.processing.util-test
  (:require [camelot.processing.util :refer :all]
            [camelot.util.application :as app]
            [midje.sweet :refer :all]))

(facts "Path Descriptions"
  (fact "Metadata can be described with a keyword matching corresponding to its path"
    (let [config {:language :en}]
      (path-description (app/gen-state config) [:camera :make]) => "Camera Make"
      (path-description (app/gen-state config) [:datetime]) => "Date/Time")))
