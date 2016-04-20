(ns camelot.core
  (:require [camelot.handler.albums :as ha]
            [camelot.handler.settings :as hs]
            [camelot.handler.surveys :as hsurv]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [camelot.util.transit :as tutil]
            [camelot.db :as db]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response]]
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
  (GET "/default-config" [] (response (cursorise (config))))
  (GET "/application" [] (response {:version (hs/get-version)
                                    :nav (hs/get-nav-menu (gen-state (config)))}))
  (GET "/settings" []
       (response (hs/settings-schema (gen-state (config)))))
  (POST "/settings" {{config :config} :params}
        (response (hs/settings-save (decursorise config))))
  (GET "/albums" []
       (let [conf (config)]
         (response (ha/read-albums (gen-state conf)
                                   (:root-path conf)))))
;;  (GET "/surveys" [] (response (hsurv/get-all (gen-state (config)))))
;;  (GET "/survey/:id" [id] (response (hsurv/get-specific (gen-state (config)) id)))
;;  (POST "/survey" {{sid :id  sname :name sdir :directory} :params}
;;        (hsurv/update! (gen-state (config)) sid sname sdir))
;;  (PUT "/survey" {{sname :name sdir :directory} :params}
;;       (hsurv/create! (gen-state (config)) sname sdir))
;;  (DELETE "/survey" {{sid :id} :params}
;;          (hsurv/delete! (gen-state (config)) sid))
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
    ;;(db/migrate)
    (println (format "Server started.  Please open http://localhost:%d/ in a browser" port))
    (run-jetty http-handler {:port port :join? false})))
