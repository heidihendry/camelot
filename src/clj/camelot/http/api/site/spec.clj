(ns camelot.http.api.site.spec
  (:require
   [camelot.spec.model.site :as model-spec]
   [camelot.http.api.spec.core :as api-core]
   [clojure.spec.alpha :as s]))

(s/def ::id ::model-spec/site-id)
(s/def ::created nat-int?)
(s/def ::updated nat-int?)
(s/def ::name ::model-spec/site-name)
(s/def ::sublocation ::model-spec/site-sublocation)
(s/def ::city ::model-spec/site-city)
(s/def ::stateProvince ::model-spec/site-state-province)
(s/def ::country ::model-spec/site-country)
(s/def ::area ::model-spec/site-area)
(s/def ::notes ::model-spec/site-notes)

(s/def ::attributes
  (s/keys :req-un [::created
                   ::updated
                   ::name]
          :opt-un [::sublocation
                   ::city
                   ::stateProvince
                   ::country
                   ::area
                   ::notes]))

(s/def ::data
  (s/keys :req-un [::api-core/id
                   ::api-core/type
                   ::attributes]))

(s/def :camelot.http.api.site.patch/attributes
  (s/keys :opt-un [::name
                   ::sublocation
                   ::city
                   ::stateProvince
                   ::country
                   ::area
                   ::notes]))

(s/def :camelot.http.api.site.patch/data
  (s/keys :req-un [::api-core/id
                   ::api-core/type
                   :camelot.http.api.site.patch/attributes]))

(s/def :camelot.http.api.site.post/attributes
  (s/keys :req-un [::name]
          :opt-un [::sublocation
                   ::city
                   ::stateProvince
                   ::country
                   ::area
                   ::notes]))

(s/def :camelot.http.api.site.post/data
  (s/keys :req-un [::api-core/type
                   :camelot.http.api.site.post/attributes]))

(s/def :camelot.http.api.site.get-all/data
  (s/coll-of ::data))
