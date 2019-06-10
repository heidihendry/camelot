(ns camelot.http.api.spec.core
  "Specs for the Camelot REST API."
  (:require [camelot.http.api.spec.error :as error]
            [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::type string?)
(s/def ::self string?)
(s/def ::status string?)
(s/def ::meta (s/keys))

;; TODO keys are not strictly defined
(s/def ::links (s/keys :req-un [::self]))

(s/def ::json-api-without-data
  (s/keys :opt-un [::error/errors
                   ::links
                   ::meta]))
