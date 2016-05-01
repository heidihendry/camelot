(ns camelot.model.trap-station
  (:require [schema.core :as s]))

(def TrapStationCreate
  {:trap-station-name s/Str
   :survey-site-id s/Num
   :trap-station-longitude (s/maybe s/Num)
   :trap-station-latitude (s/maybe s/Num)
   :trap-station-altitude (s/maybe s/Num)
   :trap-station-sublocation (s/maybe s/Str)
   :trap-station-notes (s/maybe s/Str)})

(def TrapStation
  (merge TrapStationCreate
         {:trap-station-id s/Num
          (s/optional-key :site-name) (s/maybe s/Str)
          :trap-station-created org.joda.time.DateTime
          :trap-station-updated org.joda.time.DateTime}))
