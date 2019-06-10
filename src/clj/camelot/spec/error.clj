(ns camelot.spec.error
  (:require [clojure.spec.alpha :as s]))

(s/def :error/type #{:error.type/not-found
                     :error.type/bad-request
                     :error.type/conflict
                     :error.type/internal})

(s/def ::error (s/keys :req [:error/type]))
