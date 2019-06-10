(ns camelot.http.api.spec.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::id string?)
(s/def ::status string?)
(s/def ::about string?)
(s/def ::link (s/keys :req-un [::about]))
(s/def ::links (s/coll-of ::link))
(s/def ::code string?)
(s/def ::title string?)
(s/def ::detail string?)
(s/def ::meta (s/keys))

(s/def ::object
  (s/keys :opt-un [::id
                   ::links
                   ::status
                   ::code
                   ::title
                   ::detail
                   ;; TODO :error/source
                   ::meta]))

(s/def ::errors (s/coll-of ::object))
