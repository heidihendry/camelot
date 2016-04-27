(ns camelot.model.survey
  (:require [schema.core :as s]))

(def SurveyCreate
  {:survey-name s/Str
   :survey-directory java.io.File})

(def SurveyUpdate
  (merge SurveyCreate
         {:survey-id s/Num}))

(def Survey
  (merge SurveyUpdate
         {:survey-created org.joda.time.DateTime
          :survey-updated org.joda.time.DateTime}))
