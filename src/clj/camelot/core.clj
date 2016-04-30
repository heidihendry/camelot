(ns camelot.core
  (:require [camelot.handler.albums :as ha]
            [camelot.handler.settings :as hs]
            [camelot.handler.surveys :as hsurvey]
            [camelot.handler.sites :as hsite]
            [camelot.handler.cameras :as hcamera]
            [camelot.handler.screens :as screens]
            [camelot.analysis.maxent :as ame]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [camelot.util.transit :as tutil]
            [camelot.db :as db]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as r]
            [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
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
  (GET "/default-config" [] (r/response (cursorise (config))))
  (GET "/metadata" [] (r/response (hs/get-metadata (gen-state (config)))))
  (GET "/application" [] (r/response {:version (hs/get-version)
                                    :nav (hs/get-nav-menu (gen-state (config)))}))
  (GET "/maxent" []
       (let [conf (config)
             data (ame/species-location-csv (gen-state conf)
                                            (ha/read-albums (gen-state conf)
                                                            (:root-path conf)))]
          (-> (r/response data)
              (r/content-type "text/csv; charset=utf-8")
              (r/header "Content-Length" (count data))
              (r/header "Content-Disposition" "attachment; filename=\"maxent.csv\""))))
  (GET "/screens" []
       (r/response (screens/all-screens (gen-state (config)))))
  (POST "/settings" {{data :data} :params}
        (r/response (hs/settings-save (decursorise data))))
  (GET "/albums" []
       (let [conf (config)]
         (r/response (ha/read-albums (gen-state conf)
                                     (:root-path conf)))))

  (GET "/survey" [] (r/response (hsurvey/get-all (gen-state (config)))))
  (GET "/survey/:id" [id] (r/response (cursorise (hsurvey/get-specific (gen-state (config)) id))))
  (POST "/survey" [data]
        (r/response (cursorise (hsurvey/update! (gen-state (config)) (decursorise data)))))
  (PUT "/survey" [data]
       (r/response (cursorise (hsurvey/create! (gen-state (config)) (decursorise data)))))
  (DELETE "/survey/:id" [id]
          (r/response {:data (hsurvey/delete! (gen-state (config)) id)}))

  (GET "/site" [] (r/response (hsite/get-all (gen-state (config)))))
  (GET "/site/:id" [id] (r/response (cursorise (hsite/get-specific (gen-state (config)) id))))
  (POST "/site" [data]
        (r/response (cursorise (hsite/update! (gen-state (config)) (decursorise data)))))
  (PUT "/site" [data]
       (r/response (cursorise (hsite/create! (gen-state (config)) (decursorise data)))))
  (DELETE "/site/:id" [id]
          (r/response {:data (hsite/delete! (gen-state (config)) id)}))

  (GET "/camera" [] (r/response (hcamera/get-all (gen-state (config)))))
  (GET "/camera/:id" [id] (r/response (cursorise (hcamera/get-specific (gen-state (config)) id))))
  (POST "/camera" [data]
        (r/response (cursorise (hcamera/update! (gen-state (config)) (decursorise data)))))
  (PUT "/camera" [data]
       (let [data (decursorise data)]
         (r/response
          (cursorise (hcamera/create! (gen-state (config))
                                      (assoc data :camera-status
                                             (read-string (:camera-status data))))))))
  (DELETE "/camera/:id" [id]
          (r/response {:data (hcamera/delete! (gen-state (config)) id)}))

  (POST "/quit" [] (System/exit 0))
  (resources "/"))

(def http-handler
  "Handler for HTTP requests"
  (do
    (-> routes
        (wrap-transit-response {:encoding :json, :opts tutil/transit-write-options})
        (wrap-transit-params {:opts tutil/transit-read-options})
        (wrap-stacktrace-log)
        (wrap-defaults api-defaults)
        wrap-with-logger
        wrap-gzip)))

(defn -main [& [mode directory]]
  (let [port (Integer. (or (env :camelot-port) 8080))]
    (do
      ;;(db/migrate)
      (println (format "Server started.  Please open http://localhost:%d/ in a browser" port))
      (run-jetty http-handler {:port port :join? false}))))
