(ns camelot.http.survey
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.survey :as survey]
   [camelot.util.crud :as crud]))

(def routes
  (context "/surveys" {state :state}
           (GET "/" [] (crud/list-resources survey/get-all :survey state))
           (GET "/:id" [id] (crud/specific-resource survey/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource survey/update! id
                                                       survey/tsurvey data state))
           (POST "/" [data] (crud/create-resource survey/create!
                                                  survey/tsurvey data state))
           (DELETE "/:id" [id] (crud/delete-resource survey/delete! id state))))
