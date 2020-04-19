(ns camelot.http.media
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [ring.util.response :as r]
   [camelot.model.media :as media]
   [camelot.util.crud :as crud]))

(def routes
  (context "/media" {state :state}
           (GET "/trap-station-session-camera/:id" [id] (crud/list-resources media/get-all :media id state))
           (GET "/:id" [id] (crud/specific-resource media/get-specific id state))
           (PUT "/:id" [id data] (crud/update-resource media/update! id media/tmedia data state))
           (POST "/" [data] (crud/create-resource media/create!
                                                  media/tmedia data state))
           (DELETE "/" [data] (r/response (media/delete-with-ids!
                                           state
                                           (:media-ids data))))
           (DELETE "/:id" [id] (crud/delete-resource media/delete! id state))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file state
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file state
                                                                                         filename (keyword style))})))

