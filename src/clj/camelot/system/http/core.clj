(ns camelot.system.http.core
  (:require
   [clojure.tools.logging :as log]
   [camelot.system.http.transit :as transit]
   [camelot.system.state :as state]
   [compojure.core :as compojure]
   [camelot.http.core :as http]
   [camelot.http.api.core :as api]
   [camelot.util.network :as network]
   [camelot.util.version :as version]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :refer [run-jetty]]
   [muuntaja.middleware :as muuntaja-middleware]
   [camelot.system.http.log :refer [wrap-with-logger]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.gzip :refer [wrap-gzip]])
  (:import (org.eclipse.jetty.server Server)))

(defonce jetty (atom nil))
(defonce cookie-store-key "insecureinsecure")

(defn wrap-system
  [handler & [options]]
  (fn [request]
    (handler (merge-with merge request {:system @state/system}))))

(defn errors-to-internal-server-error
  [handler & options]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
          {:status 500
           :headers {}
           :body (.getMessage e)}))))

(def http-handler
  (compojure/routes
   (-> http/app-routes
       wrap-params
       wrap-system
       errors-to-internal-server-error
       wrap-multipart-params
       (wrap-session {:store (cookie-store {:key cookie-store-key})})
       (wrap-transit-response {:encoding :json, :opts transit/transit-write-options})
       (wrap-transit-params {:opts transit/transit-read-options})
       wrap-stacktrace-log
       (wrap-defaults api-defaults)
       wrap-with-logger
       wrap-gzip)
   (-> api/core-api
       wrap-system
       muuntaja-middleware/wrap-format
       errors-to-internal-server-error
       wrap-multipart-params
       (wrap-session {:store (cookie-store {:key cookie-store-key})})
       wrap-stacktrace-log
       wrap-json-response
       (wrap-defaults api-defaults)
       wrap-with-logger
       wrap-gzip)))

(defrecord HttpServer [http-port]
  component/Lifecycle
  (start [this]
    (if @jetty
      (do
        (log/warn "Jetty already running; not starting.")
        (assoc this :jetty @jetty))
      (do
        (println (format "Camelot %s started on port %d.\n" (version/get-version)
                         http-port))
        (println "You might be able to connect to it from the following addresses:")
        (network/print-network-addresses http-port)
        (let [j (run-jetty #'http-handler {:port http-port :join? false})]
          (reset! jetty j)
          (assoc this :jetty j)))))

  (stop [this]
    (when (get this :jetty)
      (.stop ^Server (get this :jetty))
      (reset! jetty nil))
    (assoc this :jetty nil)))
