(ns camelot.model.survey
  (:require [schema.core :as s]))

(def SurveyCreate
  {:survey-name s/Str
   :survey-directory s/Str
   :survey-sampling-point-density (s/maybe s/Num)
   :survey-notes (s/maybe s/Str)})

(def Survey
  (merge SurveyCreate
         {:survey-id s/Num
          :survey-created org.joda.time.DateTime
          :survey-updated org.joda.time.DateTime}))
