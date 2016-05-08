(ns camelot.handler.screens
  (:require [camelot.processing.screens :as screens]
            [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [ring.util.response :as r]))

(defn all-screens
  "Return settings, menu and configuration definitions"
  [state]
  (screens/smith state))

(def routes
  (context "/screens" []
           (GET "/" [] (r/response (all-screens (gen-state (config)))))))
