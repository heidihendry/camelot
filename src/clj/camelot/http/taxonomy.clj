(ns camelot.http.taxonomy
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.associated-taxonomy :as associated-taxonomy]
   [camelot.util.crud :as crud]))

(def routes
  (context "/taxonomy" {state :state}
           (GET "/" [] (crud/list-resources taxonomy/get-all :taxonomy state))
           (GET "/available/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy state))
           (GET "/alternatives/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy state))
           (GET "/:id" [id] (crud/specific-resource taxonomy/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data state))
           (GET "/survey/:id" [id] (crud/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id state))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (crud/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} state))
           (POST "/" [data] (crud/create-resource associated-taxonomy/create!
                                                  associated-taxonomy/tassociated-taxonomy data state))
           (DELETE "/:id" [id] (crud/delete-resource taxonomy/delete! id state))))
