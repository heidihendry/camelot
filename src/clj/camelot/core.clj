(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require
   [camelot.app.state :as state]
   [camelot.app.db :as db]
   [camelot.app.http :as http]
   [com.stuartsierra.component :as component]
   [environ.core :refer [env]]
   [clojure.tools.nrepl.server :as nrepl])
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))
(defn camelot
  [{:keys [cli-args options]}]
  (-> (component/system-map
       :config (state/map->Config {:store state/config-store
                                   :config (state/config)
                                   :path (state/path-map)})
       :database (db/map->Database {:connection state/spec})
       :app (http/map->HttpServer {:port (or (:port options)
                                             (Integer. (or (env :camelot-port) 5341)))
                                   :dev-mode (:dev-mode options)
                                   :cli-args (or cli-args [])}))
      (component/system-using
       {:app {:config :config
              :database :database}})))

(defn start []
  (reset! http/system (component/start (camelot {:options {:dev-mode true}})))
  nil)

(defn stop []
  (swap! http/system component/stop)
  nil)

(defn start-prod []
  (reset! http/system (component/start (camelot {})))
  nil)

(defn -main [& args]
  (camelot {:cli args})
  (start-prod))
