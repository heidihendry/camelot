(ns camelot.server
  (:require [camelot.handler.main :as main]
            [camelot.config :refer [gen-state config]]
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
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]])
  (:import [java.awt Desktop]
           [java.net URI]
           [org.joda.time DateTime])
  (:gen-class))

(defn- start-browser
  []
  (if (not (env :camelot-launch-browser-disabled))
    (let [addr "http://localhost:3000/"
          uri (new URI addr)]
      (try
        (if (Desktop/isDesktopSupported)
          (.browse (Desktop/getDesktop) uri))
        (catch java.lang.UnsupportedOperationException e
          (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &")))))))

(def joda-time-reader (transit/read-handler #(DateTime. (Long/parseLong %))))
(def file-reader (transit/read-handler #(java.io.File. (Long/parseLong %))))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   #(-> % c/to-date .getTime)
   #(-> % c/to-date .getTime .toString)))

(def file-writer
  (transit/write-handler
   (constantly "f")
   #(identity %)
   #(-> % .getPath)))

(defroutes routes
  (GET "/" _
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (io/input-stream (io/resource "public/index.html"))})
  (GET "/albums" [] (response (main/read-albums (gen-state config) "/home/chris/testdata")))
  (POST "/transit-test" {{time :t} :params} (response {:a time}))
  (resources "/"))

(def http-handler
  (do
    ;;(start-browser)
    (-> routes
        (wrap-transit-response {:encoding :json, :opts
                                {:handlers {org.joda.time.DateTime joda-time-writer
                                            java.io.File file-writer}}})
        (wrap-transit-params {:opts {:handlers {(constantly "m") joda-time-reader
                                                (constantly "f") file-reader}}})
        (wrap-defaults api-defaults)
        wrap-with-logger
        wrap-gzip)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :camelot-port) 10555))]
    (run-jetty http-handler {:port port :join? false})))
