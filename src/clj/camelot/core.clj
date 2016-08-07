(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require [camelot.util.transit :as tutil]
            [camelot.migrate :refer [migrate]]
            [camelot.routes :refer [app-routes]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [clojure.java.shell :refer [sh]])
  (:import [java.net URI]
           [java.awt Desktop])
  (:gen-class))

(defn- start-browser
  [port]
  (let [addr (str "http://localhost:" port "/")
        uri (new URI addr)]
    (try
      (if (Desktop/isDesktopSupported)
        (.browse (Desktop/getDesktop) uri))
      (catch java.lang.UnsupportedOperationException e
        (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &"))))))

(def http-handler
  "Handler for HTTP requests"
  (-> app-routes
      wrap-params
      wrap-multipart-params
      (wrap-transit-response {:encoding :json, :opts tutil/transit-write-options})
      (wrap-transit-params {:opts tutil/transit-read-options})
      wrap-stacktrace-log
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defn -main [& args]
  (let [port (Integer. (or (env :camelot-port) 8080))]
    (migrate)
    (println (format "Server started.  Please open http://localhost:%d/ in a browser" port))
    (when (some #(= "--browser" %) args)
      (start-browser port))
    (run-jetty http-handler {:port port :join? false})))
