(ns camelot.spec.system
  "System specs."
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::figwheel boolean?)
(s/def ::browser boolean?)
(s/def ::port number?)

;; should use the real things, but currently causes cyclic dep.
(s/def ::config  (s/keys))
(s/def ::database (s/keys :req-un []))
(s/def ::app (s/keys :req-un [::port ::browser] :opt-un [::figwheel]))
(s/def ::jetty (s/keys))
(s/def ::importer (s/keys))

(s/def ::dataset-id (s/with-gen keyword? #(s/gen #{:default})))
(s/def ::session (s/keys :req-un [::dataset-id]))

(s/def ::state
  (s/keys :req-un [::config ::database ::app ::session]
          :opt-un [::jetty ::importer ::figwheel]))
