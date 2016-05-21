(ns smithy.core
  (:require [smithy.impl.components :as components]))

(defn build-view-component
  "Return a root view-component for `type'.
`type' should correspond to a type in the schema."
  [type]
  (components/build-view-component type))
