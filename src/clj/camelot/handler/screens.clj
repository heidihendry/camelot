(ns camelot.handler.screens
  (:require [camelot.model.screens :as model]
            [camelot.translation.core :as tr]
            [camelot.util.config :as conf]
            [camelot.util.application :as app]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]
            [smithy.core :as smithy]))

(defn translate-fn
  "Return a key translation function for the smithy build process."
  [state]
  (fn [resource lookup]
    (tr/translate (:config state) (keyword (format "%s/%s"
                                                   (name resource)
                                                   (subs (str lookup) 1))))))

(defn all-screens
  "Build the available screen smiths."
  [state]
  (smithy/build-smiths model/smiths (translate-fn state) state))

(def routes
  (context "/screens" []
           (GET "/" [] (r/response (all-screens (app/gen-state (conf/config)))))))
