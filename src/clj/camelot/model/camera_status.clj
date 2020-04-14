(ns camelot.model.camera-status
  "Camera status model and data access."
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]
   [camelot.translation.core :as tr]))

(def query (db/with-db-keys :camera-status))

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

(sch/defrecord TCameraStatus
    [camera-status-is-deployed :- sch/Bool
     camera-status-is-terminated :- sch/Bool
     camera-status-description :- sch/Str]
  {sch/Any sch/Any})

(sch/defrecord CameraStatus
    [camera-status-id :- sch/Num
     camera-status-is-deployed :- sch/Bool
     camera-status-is-terminated :- sch/Bool
     camera-status-description :- sch/Str]
  {sch/Any sch/Any})

(def camera-status map->CameraStatus)
(def tcamera-status map->TCameraStatus)

(sch/defn get-all :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses."
  [state :- State]
  (->> (query state :get-all)
       (translate-statuses state)
       (map camera-status)))

(sch/defn get-all-raw :- [CameraStatus]
  "Retrieve, translate and return all available camera statuses without translating."
  [state :- State]
  (->> (query state :get-all)
       (map camera-status)))

(sch/defn get-specific-with-description :- (sch/maybe CameraStatus)
  "Return a camera status with the given description, should one exist."
  [state :- State
   desc :- sch/Str]
  (->> (get-all-raw state)
       (filter #(= desc (:camera-status-description %)))
       (first)))

(defn- get-status-by-name
  "Get the status ID given the status's name."
  [state status]
  (->> (str "camera-status/" status)
       (get-specific-with-description state)
       :camera-status-id))

(sch/defn active-status-id
  "Return the status ID for the 'active' status"
  [state]
  (get-status-by-name state "active"))

(sch/defn available-status-id
  "Return the status ID for the 'available' status"
  [state]
  (get-status-by-name state "available"))
