(ns camelot.spec.model.survey
  (:require [clojure.spec.alpha :as s]
            [clj-time.spec :as ts]))

(s/def ::survey-id int?)
(s/def ::survey-created ::ts/date-time)
(s/def ::survey-updated ::ts/date-time)
(s/def ::survey-name string?)
(s/def ::survey-sighting-independence-threshold (s/and number?
                                                       (s/or :zero zero? :pos pos?)))
(s/def ::survey-notes (s/nilable string?))

(s/def ::survey
  (s/keys :req-un [::survey-id
                   ::survey-created
                   ::survey-updated
                   ::survey-name
                   ::survey-sighting-independence-threshold]
          :opt-un [::survey-notes]))
