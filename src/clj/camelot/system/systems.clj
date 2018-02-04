(ns camelot.system.systems
  "Camelot's available systems."
  (:require
   [camelot.system.cli :as cli]
   [camelot.system.http :as http]
   [camelot.system.db :as db]
   [camelot.system.state :as state]
   [camelot.system.maintenance :as maintenance]
   [com.stuartsierra.component :as component]
   [camelot.system.importer :as importer]
   [clojure.tools.logging :as log]))

(defn- init
  [{:keys [restore-db]}]
  (let [dbspec (if restore-db
                 (assoc state/spec :restoreFrom restore-db)
                 state/spec)
        smap (component/system-map
              :config (state/map->Config {:store state/config-store
                                          :config (state/config)
                                          :path (state/path-map)})
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
  ([]
   (pre-init {}))
  ([payload]
   (log/info "Checking database...")
   (let [db-initd? (maintenance/is-db-initialised?)
         system (component/start (init payload))]
     (check-and-migrate-db system db-initd?)
     system)))

(defn- camelot-system
  [{:keys [port browser]}]
  (let [smap (component/system-map
              :config (state/map->Config {:store state/config-store
                                          :config (state/config)
                                          :path (state/path-map)})
              :database (db/map->Database {:connection state/spec})
              :importer (importer/map->Importer {})
              :app (http/map->HttpServer
                    {:port (or port (:port cli/option-defaults))
                     :browser (or browser (:browser cli/option-defaults))}))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn camelot
  [opts]
  (component/start (camelot-system opts)))

(defn- maintenance-system
  [{:keys [port browser]}]
  (let [smap (component/system-map
              :config (state/map->Config {:store state/config-store
                                          :config (state/config)
                                          :path (state/path-map)})
              :database (db/map->Database {:connection state/spec})
              :app (http/map->HttpServer
                    {:port (or port (:port cli/option-defaults))
                     :browser (or browser (:browser cli/option-defaults))}))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn maintenance
  [opts]
  (component/start (maintenance-system opts)))
