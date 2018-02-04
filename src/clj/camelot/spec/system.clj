(ns camelot.spec.system
  "System specs."
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::connection (s/keys))

;; should use the real things, but currently causes cyclic dep.
(s/def ::config  (s/keys))
(s/def ::database (s/keys :req-un [::connection]))
(s/def ::app (s/keys))
(s/def ::jetty (s/keys))
(s/def ::importer (s/keys))

(s/def ::state
  (s/keys :req-un [::config ::database ::app]
          :opt-un [::jetty ::importer]))
