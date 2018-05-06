(ns camelot.http.settings
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET PUT]]
   [camelot.util.state :as state]
   [camelot.util.cursorise :as cursorise]))

(def routes
  (context "/settings" {session :session state :system}
           (GET "/" [] (r/response (cursorise/cursorise (merge (deref (get-in state [:config :store])) session))))
           (PUT "/" [data] (assoc (r/response (state/save-config (cursorise/decursorise data)))
                                  :session {:language (:value (:language data))}))))
