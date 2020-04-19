(ns camelot.http.survey-file
  (:require
   [clojure.edn :as edn]
   [ring.util.response :as r]
   [compojure.core :refer [context DELETE GET POST]]
   [camelot.model.survey-file :as survey-file]
   [camelot.util.crud :as crud]))

(def routes
  (context "/files" {state :state}
           (GET "/survey/:id" [id] (crud/list-resources survey-file/get-all :survey-file id state))
           (GET "/survey/:id/file/:file-id" [id file-id] (crud/specific-resource survey-file/get-specific
                                                                           file-id state))
           (GET "/survey/:id/file/:file-id/download" [id file-id]
                (survey-file/download state
                                      (edn/read-string file-id)))
           (DELETE "/survey/:id/file/:file-id" [id file-id] (crud/delete-resource survey-file/delete!
                                                                            file-id state))
           (POST "/" {params :multipart-params}
                 (r/response (survey-file/upload! state
                                                  (edn/read-string (get params "survey-id"))
                                                  (get params "file"))))))
