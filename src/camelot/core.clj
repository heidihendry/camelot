(ns camelot.core
  (:gen-class)
  (:require [camelot.reader.dirtree :as r]
            [camelot.album :as a]
            [camelot.config :refer [gen-state config]]
            [camelot.problems :as problems]
            [camelot.action.rename-photo :as rp]
            [clojure.java.shell :refer [sh]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as rsp]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:import [java.awt Desktop]
           [java.net URI]))

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

(defroutes app-routes
  (GET "/" [] (rsp/redirect "/index.html"))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn- start-browser
  []
  (let [addr "http://localhost:3000/"
          uri (new URI addr)]
      (try
        (if (Desktop/isDesktopSupported)
          (.browse (Desktop/getDesktop) uri))
        (catch java.lang.UnsupportedOperationException e
          (sh "bash" "-c" (str "xdg-open " addr " 1> /dev/null 2>&1  &"))))))

(def app
  (do
    (start-browser)
    (wrap-defaults app-routes site-defaults)))
