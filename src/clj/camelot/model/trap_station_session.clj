(ns camelot.model.trap-station-session
  (:require [schema.core :as s]))

(def TrapStationSessionCreate
  {:trap-station-id s/Num
   :trap-station-session-start-date org.joda.time.DateTime
   :trap-station-session-end-date org.joda.time.DateTime
   :trap-station-session-notes (s/maybe s/Str)})

(def TrapStationSession
  (merge TrapStationSessionCreate
         {:trap-station-session-id s/Num
          :trap-station-session-created org.joda.time.DateTime
          :trap-station-session-updated org.joda.time.DateTime}))

(def TrapStationSessionLabeled
  (merge TrapStationSession
         {:trap-station-session-label s/Str}))
