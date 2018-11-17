(ns camelot.http.app
  (:require
   [clojure.java.io :as io]
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST]]
   [camelot.model.screens :as screens]
   [camelot.util.db-migrate :as db-migrate]
   [camelot.util.version :as version]))

(defn- retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "www/index.html"))})

(defn- retrieve-favicon
  "Return a response for index.html"
  []
  {:status 404
   :headers {"Content-Type" "application/octet-stream; charset=utf-8"}
   :body nil})

(defn- heartbeat
  [state]
  (let [conn (get-in state [:database :connection])]
    {:status 200
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body (format "Status: OK\nSoftware version: %s\nDatabase version: %s\n"
                   (version/get-version)
                   (db-migrate/version conn))}))

(def routes
  (context "" {session :session state :system}
           (GET "/" _ (retrieve-index))
           (GET "/favicon.ico" _ (retrieve-favicon))
           (GET "/application" []
                (r/response {:version (version/get-version)
                             :nav (screens/nav-menu (assoc state :session session))}))
           (GET "/screens" []
                (r/response (screens/all-screens (assoc state :session session))))
           (GET "/heartbeat" []
                (heartbeat state))
           (POST "/quit" [] (System/exit 0))
           (GET "/quit" [] (System/exit 0))))
