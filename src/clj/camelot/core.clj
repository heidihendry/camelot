(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require [camelot.util.transit :as tutil]
            [camelot.db :as db]
            [camelot.routes :refer [app-routes]]
            [camelot.report-builder.module.loader :as module.loader]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]])
  (:gen-class))

(def http-handler
  "Handler for HTTP requests"
  (-> app-routes
      (wrap-transit-response {:encoding :json, :opts tutil/transit-write-options})
      (wrap-transit-params {:opts tutil/transit-read-options})
      (wrap-stacktrace-log)
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defn -main [& [mode directory]]
  (let [port (Integer. (or (env :camelot-port) 8080))]
    (db/migrate)
    (module.loader/load-user-modules)
    (println (format "Server started.  Please open http://localhost:%d/ in a browser" port))
    (run-jetty http-handler {:port port :join? false})))
