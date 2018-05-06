(ns camelot.http.site
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.site :as site]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sites" {session :session state :system}
           (GET "/" [] (crud/list-resources site/get-all :site (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource site/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource site/update! id
                                                       site/tsite data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource site/create!
                                                  site/tsite data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource site/delete! id (assoc state :session session)))))
