(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require
   [camelot.system.state :as state]
   [camelot.system.db :as db]
   [camelot.system.http :as http]
   [com.stuartsierra.component :as component]
   [environ.core :refer [env]]
   [clojure.tools.nrepl.server :as nrepl]
   [camelot.system.importer :as importer])
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))
(defn camelot
  [{:keys [cli-args options]}]
  (let [smap (component/system-map
              :config (state/map->Config {:store state/config-store
                                          :config (state/config)
                                          :path (state/path-map)})
              :database (db/map->Database {:connection state/spec})
              :importer (importer/->Importer {})
              :app (if-let [dsvr (:dev-server options)]
                     dsvr
                     (http/map->HttpServer {:port (or (:port options)
                                                      (Integer. (or (env :camelot-port) 5341)))
                                            :cli-args (or cli-args [])})))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn start-prod []
  (reset! http/system (component/start (camelot {})))
  nil)

(defn -main [& args]
  (camelot {:cli args})
  (start-prod))
