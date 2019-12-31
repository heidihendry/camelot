(ns camelot.detection.event)

(defn to-prepare-task-event
  [batch]
  {:subject :trap-station-session-camera
   :subject-id (:trap-station-session-camera-id batch)
   :action :prepare
   :payload batch})

(defn to-upload-media-event
  [m scid]
  {:subject :media
   :subject-id (:media-id m)
   :container-id scid
   :action :upload
   :payload m})

(defn to-presubmit-check-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :presubmit-check})

(defn to-submit-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :submit})

(defn to-check-result-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :check-result})

(defn to-image-detection-event
  [media-id payload]
  {:subject :image
   :subject-id media-id
   :action :detection
   :payload payload})
