(ns camelot.http.media
  (:require
   [compojure.core :refer [context DELETE GET POST PUT]]
   [ring.util.response :as r]
   [camelot.model.media :as media]
   [camelot.util.crud :as crud]))

(def routes
  (context "/media" {session :session state :system}
           (GET "/trap-station-session-camera/:id" [id] (crud/list-resources media/get-all :media id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource media/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource media/update! id media/tmedia data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource media/create!
                                                  media/tmedia data (assoc state :session session)))
           (DELETE "/" [data] (r/response (media/delete-with-ids!
                                           (assoc state :session session)
                                           (:media-ids data))))
           (DELETE "/:id" [id] (crud/delete-resource media/delete! id (assoc state :session session)))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file (assoc state :session session)
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file (assoc state :session session)
                                                                                         filename (keyword style))})))

