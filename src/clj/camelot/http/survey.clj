(ns camelot.http.survey
  (:require
   [ring.util.response :as r]
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.survey :as survey]
   [camelot.util.crud :as crud]))

(def routes
  (context "/surveys" {session :session state :system}
           (GET "/" [] (crud/list-resources survey/get-all :survey (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource survey/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource survey/update! id
                                                       survey/tsurvey data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource survey/create!
                                                  survey/tsurvey data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource survey/delete! id (assoc state :session session)))))
