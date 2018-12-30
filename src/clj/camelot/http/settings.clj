(ns camelot.http.settings
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET PUT]]
   [camelot.util.cursorise :as cursorise]))

(def routes
  (context "/settings" {session :session state :system}
           (GET "/" [] (-> (get state :config)
                           (merge session)
                           cursorise/cursorise
                           r/response))))
