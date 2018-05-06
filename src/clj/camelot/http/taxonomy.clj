(ns camelot.http.taxonomy
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.associated-taxonomy :as associated-taxonomy]
   [camelot.util.crud :as crud]))

(def routes
  (context "/taxonomy" {session :session state :system}
           (GET "/" [] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource taxonomy/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data (assoc state :session session)))
           (GET "/survey/:id" [id] (crud/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id (assoc state :session session)))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (crud/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} (assoc state :session session)))
           (POST "/" [data] (crud/create-resource associated-taxonomy/create!
                                                  associated-taxonomy/tassociated-taxonomy data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource taxonomy/delete! id (assoc state :session session)))))
