(ns camelot.model.camera-status
  "Camera status model and data access."
  (:require
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.translation.core :as tr]))

(sql/defqueries "sql/camera-status.sql")

(def camera-available "camera-status/available")

(defn translate-status
  "Translate a camera status to something readable."
  ([state status]
   (tr/translate state (keyword status))))

(defn translate-statuses
  "Translate the description of camera statuses."
  [state statuses]
  (map #(update % :camera-status-description (partial translate-status state))
       statuses))

(s/defrecord TCameraStatus
    [camera-status-is-deployed :- s/Bool
     camera-status-is-terminated :- s/Bool
     camera-status-description :- s/Str]
  {s/Any s/Any})

(s/defrecord CameraStatus
    [camera-status-id :- s/Num
     camera-status-is-deployed :- s/Bool
     camera-status-is-terminated :- s/Bool
     camera-status-description :- s/Str]
  {s/Any s/Any})

(def camera-status map->CameraStatus)
(def tcamera-status map->TCameraStatus)

(s/defn get-all :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses."
  [state :- State]
  (->> (db/with-connection state -get-all)
       db/clj-keys
       (translate-statuses state)
       (map camera-status)))

(s/defn get-all-raw :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses without translating."
  [state :- State]
  (->> (db/with-connection state -get-all)
       db/clj-keys
       (map camera-status)))

(s/defn get-specific-with-description :- (s/maybe CameraStatus)
  "Return a camera status with the given description, should one exist."
  [state :- State
   desc :- s/Str]
  (->> (get-all-raw state)
       (filter #(= desc (:camera-status-description %)))
       (first)))

(defn- get-status-by-name
  "Get the status ID given the status's name."
  [state status]
  (->> (str "camera-status/" status)
       (get-specific-with-description state)
       :camera-status-id))

(s/defn active-status-id
  "Return the status ID for the 'active' status"
  [state]
  (get-status-by-name state "active"))

(s/defn available-status-id
  "Return the status ID for the 'available' status"
  [state]
  (get-status-by-name state "available"))
