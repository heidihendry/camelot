(ns camelot.model.site
  (:require [schema.core :as s]))

(def SiteCreate
  {:site-name s/Str
   :site-sublocation (s/maybe s/Str)
   :site-city (s/maybe s/Str)
   :site-state-province (s/maybe s/Str)
   :site-country (s/maybe s/Str)
   :site-notes (s/maybe s/Str)})

(def SiteUpdate
  (merge SiteCreate
         {:site-id s/Num}))

(def Site
  (merge SiteUpdate
         {:site-created org.joda.time.DateTime
          :site-updated org.joda.time.DateTime}))
