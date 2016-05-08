(ns camelot.model.camera
  (:require [schema.core :as s]))

(def CameraStatusCreate
  {:camera-status-is-deployed s/Bool
   :camera-status-is-terminated s/Bool
   :camera-status-description s/Str})

(def CameraStatus
  (merge CameraStatusCreate
         {:camera-status-id s/Num}))

(def CameraCreate
  {:camera-name s/Str
   :camera-make (s/maybe s/Str)
   :camera-model (s/maybe s/Str)
   :camera-notes (s/maybe s/Str)
   :camera-status-id s/Num})

(def Camera
  (merge CameraCreate
         {:camera-id s/Num
          :camera-created org.joda.time.DateTime
          :camera-updated org.joda.time.DateTime}))

