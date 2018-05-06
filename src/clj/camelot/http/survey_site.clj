(ns camelot.http.survey-site
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.survey-site :as survey-site]
   [camelot.util.crud :as crud]))

(def routes
  (context "/survey-sites" {session :session state :system}
           (GET "/" [] (crud/list-resources survey-site/get-all* :survey-site (assoc state :session session)))
           (GET "/survey/:id" [id] (crud/list-resources survey-site/get-all :survey-site id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource survey-site/get-specific id (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available survey-site/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available survey-site/get-alternatives id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource survey-site/delete! id (assoc state :session session)))))
