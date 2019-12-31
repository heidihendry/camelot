(ns camelot.spec.schema.state
  "State schema"
  (:require
   [schema.core :as sch]))

(def State
  {(sch/required-key :config) sch/Any
   (sch/required-key :database) sch/Any
   (sch/optional-key :app) sch/Any
   (sch/optional-key :importer) sch/Any
   (sch/optional-key :figwheel) sch/Any
   (sch/optional-key :session) sch/Any
   (sch/optional-key :jetty) sch/Any
   (sch/optional-key :camera-status-active-id) sch/Int})
