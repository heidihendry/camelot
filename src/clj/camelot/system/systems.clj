(ns camelot.system.systems
  "Available systems."
  (:require
   [camelot.system.config.core :as config]
   [camelot.system.http.core :as http]
   [camelot.system.db.core :as db]
   [camelot.system.importer :as importer]
   [camelot.system.detector :as detector]
   [camelot.util.maintenance :as maintenance]
   [camelot.util.state :as state]
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :as log]))

(defn- init-dbs
  [system-config]
  (let [state-like-unscoped (state/config->state system-config)]
    (letfn [(reducer [acc dataset-id]
              (let [state-like (state/with-dataset state-like-unscoped dataset-id)]
                (assoc acc dataset-id (state/spec state-like))))]
      (reduce reducer {} (:dataset-ids system-config)))))

(defn- init
  [system-config]
  (let [db-specs (init-dbs system-config)
        smap (component/system-map
              :config (config/map->Config system-config)
              :database (db/map->Database {:connections db-specs}))]
    (component/system-using smap {})))

(defn- check-and-migrate-db!
  [state db-initd?]
  (if (maintenance/migrations-available? state)
    (do
      (when db-initd?
        (let [plan (maintenance/upgrade-plan state)]
          (log/info (format
                     "Database %s requires an upgrade (%s => %s). Taking a backup..."
                     (state/get-dataset-id state)
                     (:from plan)
                     (:to plan))))
        (maintenance/backup state))
      (log/info (format "Upgrading database %s..." (state/get-dataset-id state)))
      (maintenance/migrate state))
    (log/info (format "Database %s up-to-date (%s)."
                      (state/get-dataset-id state)
                      (:from (maintenance/upgrade-plan state))))))

(defn pre-init
  "Carry out Camelot's pre-flight checks."
  [system-config]
  (log/info "Checking databases...")
  (let [system (component/start (init system-config))]
    (doseq [dataset-id (:dataset-ids system-config)]
      (let [state (state/with-dataset system dataset-id)
            db-initd? (maintenance/is-db-initialised? state)]
        (check-and-migrate-db! state db-initd?)))
    system))

(defn camelot-system
  [system-config]
  (let [smap (component/system-map
              :config (config/map->Config system-config)
              :database (db/map->Database {:connections (init-dbs system-config)})
              :importer (importer/map->Importer {})
              :detector (detector/map->Detector {})
              :app (if-let [dsvr (get-in system-config [:server :dev-server])]
                     dsvr
                     (http/->HttpServer (get-in system-config [:server :http-port]))))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer
                                        :detector :detector}
                                  :importer {:config :config}
                                  :detector {:config :config
                                             :database :database}})))

(defn camelot
  [system-config]
  (component/start (camelot-system system-config)))

(defn- maintenance-system
  [system-config]
  (let [smap (component/system-map
              :config (config/map->Config system-config)
              :database (db/map->Database {:connections (init-dbs system-config)})
              :app (http/map->HttpServer system-config))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn maintenance
  [config]
  (component/start (maintenance-system config)))
