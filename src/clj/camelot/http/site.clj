(ns camelot.http.site
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.site :as site]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sites" {state :state}
           (GET "/" [] (crud/list-resources site/get-all :site state))
           (GET "/:id" [id] (crud/specific-resource site/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource site/update! id
                                                       site/tsite data state))
           (POST "/" [data] (crud/create-resource site/create!
                                                  site/tsite data state))
           (DELETE "/:id" [id] (crud/delete-resource site/delete! id state))))
