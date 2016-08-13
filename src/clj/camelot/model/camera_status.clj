(ns camelot.model.camera-status
  (:require [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [schema.core :as s]
            [camelot.db :as db]
            [camelot.translation.core :as tr]
            [camelot.application :as app]
            [camelot.util.config :as config]))

(sql/defqueries "sql/camera-status.sql" {:connection db/spec})

(def camera-available "camera-status/available")

(defn translate-status
  "Translate a camera status to something readable."
  ([status]
   (tr/translate (app/gen-state (config/config))
                 (keyword status)))
  ([state status]
   (tr/translate (:config state) (keyword status))))

(defn- translate-statuses
  "Translate the description of camera statuses."
  [state statuses]
  (map #(update % :camera-status-description
                (partial translate-status state))
       statuses))

(s/defrecord TCameraStatus
    [camera-status-is-deployed :- s/Bool
     camera-status-is-terminated :- s/Bool
     camera-status-description :- s/Str])

(s/defrecord CameraStatus
    [camera-status-id :- s/Num
     camera-status-is-deployed :- s/Bool
     camera-status-is-terminated :- s/Bool
     camera-status-description :- s/Str])

(s/defn camera-status :- CameraStatus
  [{:keys [camera-status-id camera-status-is-deployed
           camera-status-is-terminated camera-status-description]}]
  (->CameraStatus camera-status-id camera-status-is-deployed
                  camera-status-is-terminated camera-status-description))

(s/defn tcamera-status :- TCameraStatus
  [{:keys [camera-status-is-deployed camera-status-is-terminated
           camera-status-description]}]
  (->TCameraStatus camera-status-is-deployed camera-status-is-terminated
                   camera-status-description))

(s/defn get-all :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses."
  [state :- State]
  (->> (db/with-connection (:connection state) -get-all)
       (db/clj-keys)
       (translate-statuses state)
       (map camera-status)))

(s/defn get-all-raw :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses without translating."
  [state :- State]
  (->> (db/with-connection (:connection state) -get-all)
       db/clj-keys
       (map camera-status)))

(s/defn get-specific-with-description :- (s/maybe CameraStatus)
  "Return a camera status with the given description, should one exist."
  [state :- State
   desc :- s/Str]
  (->> (get-all-raw state)
       (filter #(= desc (:camera-status-description %)))
       (first)))

(s/defn default-camera-status :- (s/maybe CameraStatus)
  "Return the camera status which should be assigned to new cameras."
  []
  (get-specific-with-description (app/gen-state (config/config))
                                 camera-available))
