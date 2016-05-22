(ns camelot.handler.albums
  (:require [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [ring.util.response :as r]
            [camelot.processing.album :as a]
            [camelot.processing.settings :refer [gen-state]]))

(defn get-all
  "Return all albums for the current configuration."
  []
  (let [conf (config)]
    (r/response (a/read-albums (gen-state conf) (:root-path conf)))))

(def routes
  "Album routes"
  (context "/albums" []
           (GET "/" [] (get-all))))
