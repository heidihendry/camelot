(ns camelot.http.settings
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context GET PUT]]
   [camelot.util.cursorise :as cursorise]))

(def routes
  (context "/settings" {state :state}
           (GET "/" [] (-> (get state :config)
                           ;; contains credentials
                           (dissoc :detector)
                           cursorise/cursorise
                           r/response))))
