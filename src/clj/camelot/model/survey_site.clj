(ns camelot.model.survey-site
  (:require [schema.core :as s]))

(def SurveySiteCreate
  {:survey-id s/Num
   :site-id s/Num})

(def SurveySite
  (merge SurveySiteCreate
         {:survey-site-id s/Num
          (s/optional-key :site-name) (s/maybe s/Str)
          :survey-site-created org.joda.time.DateTime
          :survey-site-updated org.joda.time.DateTime}))
