(ns camelot.core
  "Camelot - Camera Trap management software for conservation research."
  (:require
   [camelot.system.config.core :as config]
   [camelot.system.http.core :as http]
   [camelot.system.db :as db]
   [camelot.system.migrater :as migrater]
   [camelot.system.backup-manager :as backup-manager]
   [camelot.system.datasets :as datasets]
   [camelot.system.importer :as importer]
   [camelot.system.detector :as detector]
   [com.stuartsierra.component :as component])
  (:gen-class))

(defn- camelot-system
  [system-overrides]
  (let [smap
        (apply component/system-map
               (apply concat
                      (merge {:config (config/map->Config {})
                              :database (db/map->Database {})
                              :backup-manager (backup-manager/map->BackupManager {})
                              :datasets (datasets/map->Datasets {})
                              :migrater (migrater/map->Migrater {})
                              :importer (importer/map->Importer {})
                              :detector (detector/map->Detector {})
                              :app (http/->HttpServer {})}
                             system-overrides)))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer
                                        :detector :detector
                                        :datasets :datasets
                                        :backup-manager :backup-manager}
                                  :config {}
                                  :database {}
                                  :migrater {:backup-manager :backup-manager}
                                  :datasets {:config :config
                                             :database :database
                                             :migrater :migrater}
                                  :importer {:config :config}
                                  :detector {:config :config
                                             :database :database
                                             :datasets :datasets}})))

(defn start-prod
  []
  (component/start (camelot-system {})))

(defn -main [& _]
  (start-prod))
