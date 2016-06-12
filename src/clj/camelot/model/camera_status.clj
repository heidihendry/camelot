(ns camelot.model.camera-status
  (:require [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [schema.core :as s]
            [camelot.db :as db]
            [camelot.translation.core :as tr]))

(sql/defqueries "sql/camera-status.sql" {:connection db/spec})

(defn- translate-statuses
  "Translate the description of camera statuses."
  [state statuses]
  (map #(assoc % :camera-status-description
               (tr/translate (:config state) (keyword (:camera-status-description %))))
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