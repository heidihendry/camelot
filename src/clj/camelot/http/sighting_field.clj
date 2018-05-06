(ns camelot.http.sighting-field
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sighting-fields" {session :session state :system}
           (GET "/" [] (crud/list-resources sighting-field/get-all :sighting-field
                                            (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource
                             sighting-field/get-specific id
                             (assoc state :session session)))
           (PUT "/:id" [id data]
                (crud/update-resource sighting-field/update! id
                                      sighting-field/tsighting-field
                                      (assoc data :sighting-field-id (edn/read-string id))
                                      (assoc state :session session)))
           (POST "/" [data]
                 (crud/create-resource sighting-field/create!
                                       sighting-field/tsighting-field
                                       data
                                       (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource sighting-field/delete! id
                                                     (assoc state :session session)))))




