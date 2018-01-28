(ns camelot.spec.db
  (:require [clojure.spec.alpha :as s]))

(s/def ::1 pos-int?)
(s/def ::execution-result (s/keys :req-un [::1]))

;; Site

(s/def ::site_id int?)
(s/def ::site_created int?)
(s/def ::site_updated int?)
(s/def ::site_name string?)
(s/def ::site_sublocation (s/nilable string?))
(s/def ::site_city (s/nilable string?))
(s/def ::site_state_province (s/nilable string?))
(s/def ::site_country (s/nilable string?))
(s/def ::site_area (s/nilable number?))
(s/def ::site_notes (s/nilable string?))

(s/def ::tsite
  (s/keys :req-un [::site_name]
          :opt-un [::site_sublocation
                   ::site_city
                   ::site_state_province
                   ::site_country
                   ::site_area
                   ::site_notes]))

(s/def ::tsite-with-id
  (s/keys :req-un [::site_name
                   ::site_id]
          :opt-un [::site_sublocation
                   ::site_city
                   ::site_state_province
                   ::site_country
                   ::site_area
                   ::site_notes]))

(s/def ::site
  (s/keys :req-un [::site_id
                   ::site_created
                   ::site_updated
                   ::site_name]
          :opt-un [::site_sublocation
                   ::site_city
                   ::site_state_province
                   ::site_country
                   ::site_area
                   ::site_notes]))

;; Camera

(s/def ::camera_id int?)
(s/def ::camera-ids (s/keys :req-un [::camera_id]))
