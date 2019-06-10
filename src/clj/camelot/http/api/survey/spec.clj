(ns camelot.http.api.survey.spec
  (:require
   [camelot.spec.model.survey :as model-spec]
   [camelot.http.api.spec.core :as api-core]
   [clojure.spec.alpha :as s]))

(s/def ::id ::model-spec/survey-id)
(s/def ::created nat-int?)
(s/def ::updated nat-int?)
(s/def ::name ::model-spec/survey-name)
(s/def ::sightingIndependenceThreshold ::model-spec/survey-sighting-independence-threshold)
(s/def ::notes ::model-spec/survey-notes)

(s/def ::attributes
  (s/keys :req-un [::created
                   ::updated
                   ::name
                   ::sightingIndependenceThreshold]
          :opt-un [::notes]))

(s/def ::data
  (s/keys :req-un [::api-core/id
                   ::api-core/type
                   ::attributes]))

(s/def :camelot.http.api.survey.patch/attributes
  (s/keys :opt-un [::name
                   ::sightingIndependenceThreshold
                   ::notes]))

(s/def :camelot.http.api.survey.patch/data
  (s/keys :req-un [::api-core/id
                   ::api-core/type
                   :camelot.http.api.survey.patch/attributes]))

(s/def :camelot.http.api.survey.post/attributes
  (s/keys :req-un [::name]
          :opt-un [::sightingIndependenceThreshold
                   ::notes]))

(s/def :camelot.http.api.survey.post/data
  (s/keys :req-un [::api-core/type
                   :camelot.http.api.survey.post/attributes]))

(s/def :camelot.http.api.survey.get-all/data
  (s/coll-of ::data))
