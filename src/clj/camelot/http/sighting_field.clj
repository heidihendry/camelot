(ns camelot.http.sighting-field
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [context DELETE GET POST PUT]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.crud :as crud]))

(def routes
  (context "/sighting-fields" {state :state}
           (GET "/" [] (crud/list-resources sighting-field/get-all :sighting-field
                                            state))
           (GET "/:id" [id] (crud/specific-resource
                             sighting-field/get-specific id
                             state))
           (PUT "/:id" [id data]
                (crud/update-resource sighting-field/update! id
                                      sighting-field/tsighting-field
                                      (assoc data :sighting-field-id (edn/read-string id))
                                      state))
           (POST "/" [data]
                 (crud/create-resource sighting-field/create!
                                       sighting-field/tsighting-field
                                       data
                                       state))
           (DELETE "/:id" [id] (crud/delete-resource sighting-field/delete! id
                                                     state))))




