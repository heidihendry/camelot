(ns camelot.handler.settings-test
  (:require [camelot.handler.settings :refer :all]
            [midje.sweet :refer :all]
            [schema.test :as st]
            [camelot.processing.settings :refer [gen-state]]))

(namespace-state-changes (before :facts st/validate-schemas))

(facts "Metadata flattening"
  (fact "Datastructure produced is vector of paths"
    (let [data metadata-paths]
      (every? identity (flatten (map #(map keyword? %) data))) => true
      (every? identity (map vector? data)) => true
      (vector? data) => true))

  (fact "An entry is available for GPS Location"
    (let [search [:location :gps-longitude]]
      (some #{search} metadata-paths) => search)))

