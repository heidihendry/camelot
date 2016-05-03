(ns camelot.model.trap-station-session-camera
  (:require [schema.core :as s]))

(def TrapStationSessionCameraCreate
  {:camera-id s/Num
   :trap-station-session-id s/Num})

(def TrapStationSessionCamera
  (merge TrapStationSessionCameraCreate
         {:trap-station-session-camera-id s/Num
          (s/optional-key :camera-name) (s/maybe s/Str)
          :trap-station-session-camera-created org.joda.time.DateTime
          :trap-station-session-camera-updated org.joda.time.DateTime}))
