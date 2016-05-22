(ns camelot.handler.albums
  (:require [camelot.processing.album :as a]
            [camelot.util.config :as conf]
            [camelot.util.application :as app]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]))

(defn get-all
  "Return all albums for the current configuration."
  []
  (let [c (conf/config)]
    (r/response (a/read-albums (app/gen-state c) (:root-path c)))))

(def routes
  "Album routes"
  (context "/albums" []
           (GET "/" [] (get-all))))
