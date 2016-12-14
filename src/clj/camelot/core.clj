(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require
   [camelot.app.transit :as transit]
   [camelot.app.routes :refer [app-routes]]
   [camelot.app.state :as state]
   [camelot.app.version :as version]
   [camelot.app.network :as network]
   [camelot.app.desktop :as desktop]
   [camelot.db.core :as db]
   [figwheel-sidecar.repl-api :as ra]
   [com.stuartsierra.component :as component]
   [environ.core :refer [env]]
   [ring.adapter.jetty :refer [run-jetty]]
   [clojure.tools.nrepl.server :as nrepl]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.logger :refer [wrap-with-logger]])
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))
(defonce system (atom {}))
(defonce jetty (atom nil))
(defonce cookie-store-key "insecureinsecure")

(defn wrap-system
  [handler & [options]]
  (fn [request]
    (handler (merge-with merge request {:system @system}))))

(def http-handler
  "Handler for HTTP requests"
  (-> app-routes
      wrap-params
      wrap-system
      wrap-multipart-params
      (wrap-session {:store (cookie-store {:key cookie-store-key})})
      (wrap-transit-response {:encoding :json, :opts transit/transit-write-options})
      (wrap-transit-params {:opts transit/transit-read-options})
      wrap-stacktrace-log
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defrecord HttpServer [cli-args dev-mode port connection config]
  component/Lifecycle
  (start [this]
    (if dev-mode
      (do
        (ra/start-figwheel!)
        (assoc this :figwheel true))
      (if @jetty
        (do
          (println "Jetty already running; not starting.")
          (assoc this :jetty @jetty))
        (do
          (println (format "Camelot %s started on port %d.\n" (version/get-version) port))
          (println "You might be able to connect to it from the following addresses:")
          (network/print-network-addresses port)
          (when (some #(= "--browser" %) cli-args)
            (desktop/start-browser port))
          (let [j (run-jetty http-handler {:port port :join? false})]
            (reset! jetty j)
            (assoc this :jetty j))))))

  (stop [this]
    (when (get this :jetty)
      (.stop (get this :jetty))
      (reset! jetty nil))
    {}))

(defn camelot
  [{:keys [cli-args options]}]
  (-> (component/system-map
       :config (state/map->Config {:store state/config-store
                                   :config (state/config)
                                   :path (state/path-map)})
       :database (db/map->Database {:connection state/spec})
       :app (map->HttpServer {:port (or (:port options)
                                        (Integer. (or (env :camelot-port) 5341)))
                              :dev-mode (:dev-mode options)
                              :cli-args (or cli-args [])}))
      (component/system-using
       {:app {:config :config
              :database :database}})))

(defn start []
  (reset! system (component/start (camelot {:options {:dev-mode true}})))
  nil)

(defn stop []
  (swap! system component/stop)
  nil)

(defn start-prod []
  (reset! system (component/start (camelot {})))
  nil)

(defn -main [& args]
  (camelot {:cli args})
  (start-prod))
