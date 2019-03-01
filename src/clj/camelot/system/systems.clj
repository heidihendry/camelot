(ns camelot.system.systems
  "Available systems."
  (:require
   [camelot.system.config.core :as config]
   [camelot.system.http.core :as http]
   [camelot.system.db.core :as db]
   [camelot.system.importer :as importer]
   [camelot.util.maintenance :as maintenance]
   [camelot.util.state :as state]
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :as log]))

(defn- init
  [config {:keys [restore-db]}]
  (let [dbspec (if restore-db
                 (assoc (state/spec) :restoreFrom restore-db)
                 (state/spec))
        smap (component/system-map
              :config (config/map->Config config)
              :database (db/map->Database {:connection dbspec}))]
    (component/system-using smap {})))

(defn- check-and-migrate-db
  [state db-initd?]
  (if (maintenance/migrations-available? state)
    (do
      (when db-initd?
        (let [plan (maintenance/upgrade-plan state)]
          (log/info (format
                     "Database requires an upgrade (%s => %s). Taking a backup..."
                     (:from plan)
                     (:to plan))))
        (maintenance/backup state))
      (log/info "Upgrading database...")
      (maintenance/migrate state))
    (log/info (format "Database up-to-date (%s)."
                      (:from (maintenance/upgrade-plan state))))))

(defn pre-init
  "Carry out Camelot's pre-flight checks."
  [config payload]
  (log/info "Checking database...")
  (let [db-initd? (maintenance/is-db-initialised? config)
        system (component/start (init config payload))]
    (check-and-migrate-db system db-initd?)
    system))

(defn camelot-system
  [config]
  (let [smap (component/system-map
              :config (config/map->Config config)
              :database (db/map->Database {:connection (state/spec)})
              :importer (importer/map->Importer {})
              :app (if-let [dsvr (get-in config [:server :dev-server])]
                     dsvr
                     (http/->HttpServer (get-in config [:server :http-port]))))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn camelot
  [config]
  (component/start (camelot-system config)))

(defn- maintenance-system
  [config]
  (let [smap (component/system-map
              :config (config/map->Config config)
              :database (db/map->Database {:connection (state/spec)})
              :app (http/map->HttpServer config))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn maintenance
  [config]
  (component/start (maintenance-system config)))
