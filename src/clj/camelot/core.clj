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
   [clojure.tools.cli :refer [parse-opts]]
   [camelot.system.importer :as importer])
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))

(def option-defaults
  {:port (or (some-> (env :camelot-port) (Integer/parseInt)) 5341)
   :browser false})

(def cli-opts
  [["-p" "--port PORT" "Port number"
    :id :port
    :default (:port option-defaults)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a valid port number"]]
   [nil "--browser" "Start browser"
    :id :browser
    :default (:browser option-defaults)]])

(defn camelot
  [{:keys [port browser]}]
  (let [smap (component/system-map
              :config (state/map->Config {:store state/config-store
                                          :config (state/config)
                                          :path (state/path-map)})
              :database (db/map->Database {:connection state/spec})
              :importer (importer/map->Importer {})
              :app (http/map->HttpServer
                    {:port (or port (:port option-defaults))
                     :browser (or browser (:browser option-defaults))}))]
    (component/system-using smap {:app {:config :config
                                        :database :database
                                        :importer :importer}
                                  :importer {:config :config}})))

(defn start-prod
  ([]
   (reset! http/system (component/start (camelot {}))))
  ([options]
   (reset! http/system (component/start (camelot options)))))

(defn -main [& args]
  (start-prod (:options (parse-opts args cli-opts))))
