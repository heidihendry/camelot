(ns camelot.server
  (:require [camelot.handler.main :as main]
            [camelot.handler.settings :as hs]
            [camelot.config :refer [gen-state config get-version create-default-config decursorise]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.response :refer [response]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [cognitect.transit :as transit]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [camelot.util.java-file :refer [getPath]])
  (:import [java.awt Desktop]
           [java.net URI]
           [org.joda.time DateTime]
           [java.io File])
  (:gen-class))

(defn- start-browser
  []
  (if (not (env :camelot-launch-browser-disabled))
    (let [addr "http://localhost:3000/"
          uri (new URI addr)]
      (try
        (if (Desktop/isDesktopSupported)
          (.browse ^Desktop (Desktop/getDesktop) uri))
        (catch java.lang.UnsupportedOperationException e
          (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &")))))))

(def joda-time-reader (transit/read-handler #(c/from-long ^java.lang.Long (Long/parseLong %))))
(def file-reader (transit/read-handler #(File. ^java.lang.Long (Long/parseLong %))))

(def getTime
  #(.getTime ^java.util.Date %))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   #(-> % c/to-long)
   #(-> % c/to-long .toString)))

(def file-writer
  (transit/write-handler
   (constantly "f")
   #(identity %)
   #(-> % (getPath))))

(defn retrieve-index
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defroutes routes
  (GET "/" _ (retrieve-index))
  (GET "/settings" _ (retrieve-index))
  (GET "/dashboard" _ (retrieve-index))
  (GET "/default-config" [] (response (config)))
  (GET "/application" [] (response {:version (get-version)}))
  (POST "/settings/save" {{config :config} :params}
        (response (hs/settings-save (decursorise config))))
  (POST "/settings/get" {{config :config} :params}
        (response (hs/settings-schema (gen-state (decursorise config)))))
  (POST "/albums" {{config :config, dir :dir} :params}
        (response (main/read-albums (gen-state (decursorise config)) dir)))
  (POST "/transit-test" {{time :t} :params} (response {:a time}))
  (resources "/"))

(def http-handler
  (do
    ;;(start-browser)
    (-> routes
        (wrap-transit-response {:encoding :json, :opts
                                {:handlers {org.joda.time.DateTime joda-time-writer
                                            File file-writer}}})
        (wrap-transit-params {:opts {:handlers {"m" joda-time-reader
                                                "f" file-reader}}})
        (wrap-defaults api-defaults)
        wrap-with-logger
        wrap-gzip)))

(defn -main [& [mode directory]]
  (case mode
    "bscheck" (let [state (gen-state (config))]
                (main/consistency-check state (main/read-albums state directory)))
    "init" (create-default-config)
    (let [port (Integer. (or (env :camelot-port) 10555))]
      (run-jetty http-handler {:port port :join? false}))))
