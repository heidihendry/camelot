(ns camelot.server
  (:require [camelot.reader.dirtree :as r]
            [camelot.album :as a]
            [camelot.config :refer [gen-state config]]
            [camelot.problems :as problems]
            [camelot.action.rename-photo :as rp]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [java.awt Desktop]
           [java.net URI])
  (:gen-class))

(defn maybe-apply
  "Apply function `f` to album, so long as there aren't any warnings'"
  [f albsev]
  (do
    (when (< (problems/severities (:severity albsev)) (problems/severities :warn))
      (f (:album albsev)))
    (:severity albsev)))

(defn run-albums
  "Run `maybe-apply` across all albums, if no album poses an error."
  [state f albums]
  (let [prob-fn #(printf "%s%s: %s\n" %1 %2 %3)
        proc-fn (fn [[dir alb]] (problems/process-problems state prob-fn dir (:problems alb)))
        albsevs (map #(hash-map :album % :severity (proc-fn %)) albums)
        most-sev (reduce problems/highest-severity :okay (map :severity albsevs))]
    (if (= most-sev :error)
      albsevs
      (map #(maybe-apply f %) albsevs))))

(defn run
  "Retrieve album data and apply transformations."
  [dir]
  (let [state (gen-state config)
        album-transform #(->> %
                              (rp/rename-photos state))]
    (->> dir
         (r/read-tree state)
         (a/album-set state)
         (run-albums state album-transform))))

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

(defroutes routes
  (GET "/" _
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (io/input-stream (io/resource "public/index.html"))})
  (resources "/"))

(def http-handler
  (do
    (start-browser)
    (-> routes
        (wrap-defaults api-defaults)
        wrap-with-logger
        wrap-gzip)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (run-jetty http-handler {:port port :join? false})))
