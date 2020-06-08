(ns camelot.http.api.dataset.spec
  (:require
   [camelot.http.api.spec.core :as api-core]
   [clojure.spec.alpha :as s]))

(s/def ::name string?)
(s/def ::isConnected boolean?)

(s/def ::attributes
  (s/keys :opt-un [::name
                   ::isConnected]))

(s/def ::data
  (s/keys :req-un [::api-core/id
                   ::api-core/type
                   ::attributes]))

(s/def :camelot.http.api.dataset.get/data (s/coll-of ::data))
