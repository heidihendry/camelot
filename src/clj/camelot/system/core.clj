(ns camelot.system.core
  "System lifecycle management."
  (:require
   [camelot.system.config.core :as config]
   [camelot.system.http.core :as http]
   [camelot.system.db.core :as db]
   [camelot.system.migrater :as migrater]
   [camelot.system.datasets :as datasets]
   [camelot.system.importer :as importer]
   [camelot.system.detector :as detector]
   [com.stuartsierra.component :as component]))

(defn camelot-system
  [system-overrides]
  (let [smap
        (apply component/system-map
               (apply concat
                      (merge {:config (config/map->Config {})
                              :database (db/map->Database {})
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
                                        :datasets :datasets}
                                  :config {}
                                  :database {}
                                  :migrater {}
                                  :datasets {:config :config
                                             :database :database
                                             :migrater :migrater}
                                  :importer {:config :config}
                                  :detector {:config :config
                                             :database :database
                                             :datasets :datasets}})))
