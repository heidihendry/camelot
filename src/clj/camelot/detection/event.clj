(ns camelot.detection.event
  (:require
   [clj-time.core :as t]))

(defn to-prepare-task-event
  [batch]
  {:subject :trap-station-session-camera
   :subject-id (:trap-station-session-camera-id batch)
   :action :prepare
   :created (t/now)
   :payload batch})

(defn to-upload-media-event
  [m scid]
  {:subject :media
   :subject-id (:media-id m)
   :container-id scid
   :action :upload
   :created (t/now)
   :payload m})

(defn to-presubmit-check-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :presubmit-check
   :created (t/now)})

(defn to-submit-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :submit
   :created (t/now)})

(defn to-check-result-event
  [task-id]
  {:subject :task
   :subject-id task-id
   :action :check-result
   :created (t/now)})

(defn to-image-detection-event
  [media-id payload]
  {:subject :image
   :subject-id media-id
   :action :detection
   :payload payload
   :created (t/now)})

(defn to-archive-task-event
  [task-id]
  {:subject :task
   :action :archive
   :subject-id task-id
   :created (t/now)})
