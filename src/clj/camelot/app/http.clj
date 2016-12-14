(ns camelot.app.http
  (:require
   [camelot.app.transit :as transit]
   [camelot.app.routes :refer [app-routes]]
   [camelot.app.version :as version]
   [camelot.app.network :as network]
   [camelot.app.desktop :as desktop]
   [com.stuartsierra.component :as component]
   [figwheel-sidecar.repl-api :as ra]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.logger :refer [wrap-with-logger]]))

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
