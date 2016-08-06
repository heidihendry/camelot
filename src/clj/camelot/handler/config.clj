(ns camelot.handler.config
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.util.cursorise :as cursorise]
            [ring.util.response :as r]
            [camelot.util
             [config :as conf]]))

(defn config-save
  "Save a configuration."
  [config]
  (conf/save-config config))

(def routes
  (context "/settings" []
           (GET "/" [] (r/response (cursorise/cursorise (conf/config))))
           (PUT "/" [data] (r/response (config-save (cursorise/decursorise data))))))
