(ns camelot.application-test
  (:require [camelot.test-util.state :as state]
            [camelot.application :as sut]
            [midje.sweet :refer :all]))

(defn gen-state-helper
  [config]
  (state/gen-state (assoc config :language :en)))

(def defined-screens
  #{:camera :survey :survey-site :site :trap-station :trap-station-session
    :trap-station-session-camera :taxonomy :photo :media :sighting})

(facts "Screen smith"
  (fact "Should contain keys for all known screens"
    (set (keys (sut/all-screens (gen-state-helper {})))) => defined-screens)

  (fact "Schemas should have labels"
    (let [path [:survey :schema :survey-name :label]]
      (get-in (sut/all-screens (gen-state-helper {})) path)) => "Survey name")

  (fact "Schemas should have descriptions"
    (let [path [:site :schema :site-name :description]]
      (type (get-in (sut/all-screens (gen-state-helper {})) path)) => String))

  (fact "Schemas should have field schema types"
    (let [path [:site :schema :site-sublocation :schema :type]]
      (get-in (sut/all-screens (gen-state-helper {})) path) => :text))

  (fact "Camera schema should have a `:camera' resource type"
    (let [path [:camera :resource :type]]
      (get-in (sut/all-screens (gen-state-helper {})) path) => :camera))

  (fact "Resource title should be translated'"
    (let [path [:trap-station-session-camera :resource :title]]
      (get-in (sut/all-screens (gen-state-helper {})) path) => "Session Camera")))
