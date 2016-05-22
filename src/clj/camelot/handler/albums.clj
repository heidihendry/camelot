(ns camelot.handler.albums
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [ring.util.response :as r]
            [camelot.processing.dirtree :as dt]
            [camelot.processing.album :as a]
            [camelot.processing.settings :refer [gen-state]]
            [camelot.util.java-file :as jf]))

(defn read-albums
  "Read photo directories and return metadata structured as albums."
  [state dir]
  (let [fdir (clojure.java.io/file dir)]
    (cond
      (nil? dir) ((:translate state) :problems/root-path-missing)
      (not (and (jf/exists? fdir) (jf/readable? fdir)))
      ((:translate state) :problems/root-path-not-found)
      :else
      (->> dir
           (dt/read-tree state)
           (a/album-set state)))))

(defn get-all
  "Return all albums for the current configuration."
  []
  (let [conf (config)]
    (r/response (read-albums (gen-state conf) (:root-path conf)))))

(def routes
  "Album routes"
  (context "/albums" []
           (GET "/" [] (get-all))))
