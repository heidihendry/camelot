(ns camelot.handler.settings-test
  (:require [camelot.handler.settings :refer :all]
            [midje.sweet :refer :all]
            [schema.test :as st]
            [camelot.config :refer [gen-state]]))

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

(facts "Path Descriptions"
  (fact "Metadata can be described with a keyword matching corresponding to its path"
    (let [config {:language :en}]
      (path-description (gen-state config) [:camera :make]) => "Camera Make"
      (path-description (gen-state config) [:datetime]) => "Date/Time")))

(facts "Config Descriptions"
  (fact "Labels are created for configuration data"
    (let [config {:language :en}
          result (config-description (gen-state config) {:project-start {:type :datetime}})]
      (contains? result :project-start) => true
      (contains? (:project-start result) :label) => true))

  (fact "Descriptions are created for configuration data"
    (let [config {:language :en}
          result (config-description (gen-state config) {:project-start {:type :datetime}})]
      (contains? result :project-start) => true
      (contains? (:project-start result) :description) => true))

  (fact "Schema data is preserved"
    (let [config {:language :en}
          result (config-description (gen-state config) {:project-start {:type :datetime}})]
      (contains? result :project-start) => true
      (:schema (:project-start result)) => {:type :datetime})))

(facts "Menus"
  (fact "Label for menus get translated"
    (let [config {:language :en}
          menu [[:things]
                [:label :settings/preferences]
                [:morethings]]]
      (translate-menu-labels (gen-state config) menu) => [[:things]
                                                          [:label "Preferences"]
                                                          [:morethings]])))
