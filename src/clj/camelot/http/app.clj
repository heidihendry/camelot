(ns camelot.http.app
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [ring.util.response :as r]
   [compojure.core :refer [context GET POST]]
   [camelot.model.screens :as screens]
   [camelot.state.datasets :as datasets]
   [camelot.util.db-migrate :as db-migrate]
   [camelot.util.version :as version]
   [camelot.util.network :as network]))

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

(defn- with-dataset
  [datasets f dataset-id]
  (-> datasets
      (datasets/assoc-dataset-context dataset-id)
      f))

(defn- map-datasets
  [f datasets]
  (map #(with-dataset datasets f %) (datasets/get-available datasets)))

(defn- dataset-connection
  [datasets]
  [(datasets/get-dataset-context datasets)
   (db-migrate/version (datasets/lookup-connection datasets))])

(defn- db-versions
  [state]
  (->> state
       :datasets
       (map-datasets dataset-connection)
       (into {})))

(defn- heartbeat
  [state]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {:status "OK"
                          :software-version (version/get-version)
                          :database-versions (db-versions state)})})

(defn- runtime
  [state]
  (let [port (get-in state [:config :server :http-port])]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str
            {:alternate-urls (network/canonicalise-addresses port)})}))

(def routes
  (context "" {state :state}
           (GET "/" _ (retrieve-index))
           (GET "/favicon.ico" _ (retrieve-favicon))
           (GET "/application" []
                (r/response {:version (version/get-version)
                             :nav (screens/nav-menu state)}))
           (GET "/screens" []
                (r/response (screens/all-screens state)))
           (GET "/heartbeat" []
                (heartbeat state))
           (GET "/runtime" []
                (runtime state))
           (POST "/quit" [] (System/exit 0))))
