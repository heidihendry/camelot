(ns camelot.handler.config
  (:require [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [camelot.util
             [config :as conf]
             [rest :as rest-util]]))

(defn config-save
  "Save a configuration."
  [config]
  (conf/save-config config))

(def routes
  (context "/settings" []
           (GET "/" [] (r/response (rest-util/cursorise (conf/config))))
           (PUT "/" [data] (r/response (config-save (rest-util/decursorise data))))))
