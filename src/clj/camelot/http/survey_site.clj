(ns camelot.http.survey-site
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.survey-site :as survey-site]
   [camelot.util.crud :as crud]))

(def routes
  (context "/survey-sites" {state :state}
           (GET "/" [] (crud/list-resources survey-site/get-all* :survey-site state))
           (GET "/survey/:id" [id] (crud/list-resources survey-site/get-all :survey-site id state))
           (GET "/:id" [id] (crud/specific-resource survey-site/get-specific id state))
           (GET "/available/:id" [id] (crud/list-available survey-site/get-available id state))
           (GET "/alternatives/:id" [id] (crud/list-available survey-site/get-alternatives id state))
           (PUT "/:id" [id data] (crud/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data state))
           (POST "/" [data] (crud/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data state))
           (DELETE "/:id" [id] (crud/delete-resource survey-site/delete! id state))))
