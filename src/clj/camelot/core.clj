(ns camelot.core
  (:require [camelot.handler.albums :as ha]
            [camelot.handler.settings :as hs]
            [camelot.processing.settings :refer [gen-state config decursorise]]
            [camelot.util.transit :as tutil]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]])
  (:gen-class))

(defn retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defroutes routes
  (GET "/" _ (retrieve-index))
  (GET "/settings" _ (retrieve-index))
  (GET "/dashboard" _ (retrieve-index))
  (GET "/default-config" [] (response (config)))
  (GET "/application" [] (response {:version (hs/get-version)}))
  (POST "/settings/save" {{config :config} :params}
        (response (hs/settings-save (decursorise config))))
  (POST "/settings/get" {{config :config} :params}
        (response (hs/settings-schema (gen-state (decursorise config)))))
  (POST "/albums" {{config :config} :params}
        (response (ha/read-albums (gen-state (decursorise config)) (:root-path (decursorise config)))))
  (POST "/transit-test" {{time :t} :params} (response {:a time}))
  (resources "/"))

(def http-handler
  "Handler for HTTP requests"
  (do
    (-> routes
        (wrap-transit-response {:encoding :json, :opts tutil/transit-write-options})
        (wrap-transit-params {:opts tutil/transit-read-options})
        (wrap-defaults api-defaults)
        wrap-with-logger
        wrap-gzip)))

(defn -main [& [mode directory]]
  (let [port (Integer. (or (env :camelot-port) 8080))]
    (println (format "Server started.  Please open http://localhost:%d/ in a browser" port))
    (run-jetty http-handler {:port port :join? false})))
