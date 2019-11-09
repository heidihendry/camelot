(ns camelot.spec.model.site
  (:require [clojure.spec.alpha :as s]
            [clj-time.spec :as tspec]))

(s/def ::site-id int?)
(s/def ::site-created ::tspec/date-time)
(s/def ::site-updated ::tspec/date-time)
(s/def ::site-name string?)
(s/def ::site-sublocation (s/nilable string?))
(s/def ::site-city (s/nilable string?))
(s/def ::site-state-province (s/nilable string?))
(s/def ::site-country (s/nilable string?))
(s/def ::site-area (s/nilable number?))
(s/def ::site-notes (s/nilable string?))

(s/def ::psite
  (s/keys :opt-un [::site-name
                   ::site-sublocation
                   ::site-city
                   ::site-state-province
                   ::site-country
                   ::site-area
                   ::site-notes]))

(s/def ::tsite
  (s/keys :req-un [::site-name]
          :opt-un [::site-sublocation
                   ::site-city
                   ::site-state-province
                   ::site-country
                   ::site-area
                   ::site-notes]))

(s/def ::site
  (s/keys :req-un [::site-id
                   ::site-created
                   ::site-updated
                   ::site-name]
          :opt-un [::site-sublocation
                   ::site-city
                   ::site-state-province
                   ::site-country
                   ::site-area
                   ::site-notes]))
