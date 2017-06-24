(ns smithy.core
  (:require [smithy.impl.components :as components]))

(defn build-view-component
  "Return a root view-component for `type'.
`type' should correspond to a type in the schema."
  [type]
  (components/build-view-component type))

(def content-view-component components/content-view-component)
(def settings-view-component components/settings-view-component)
