(ns camelot.app.routes
  (:require
   [camelot.app.screens :as screens]
   [camelot.app.state :as state]
   [camelot.app.version :as version]
   [camelot.import.capture :as capture]
   [camelot.import.core :as import]
   [camelot.import.album :as album]
   [camelot.bulk-import.core :as bulk-import]
   [camelot.import.db :as im.db]
   [camelot.services.species-search :as species-search]
   [camelot.db.associated-taxonomy :as ataxonomy]
   [camelot.db.camera :as camera]
   [camelot.db.camera-status :as camera-status]
   [camelot.db.media :as media]
   [camelot.db.photo :as photo]
   [camelot.db.sighting :as sighting]
   [camelot.db.site :as site]
   [camelot.db.taxonomy :as taxonomy]
   [camelot.db.species-mass :as species-mass]
   [camelot.db.survey :as survey]
   [camelot.db.survey-site :as survey-site]
   [camelot.db.survey-file :as survey-file]
   [camelot.db.trap-station :as trap-station]
   [camelot.db.trap-station-session :as trap-station-session]
   [camelot.db.trap-station-session-camera :as trap-station-session-camera]
   [camelot.db.deployment :as deployment]
   [camelot.db.camera-deployment :as camera-deployment]
   [camelot.db.library :as library]
   [camelot.report.core :as report]
   [camelot.app.crud-util :as crud]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [compojure.core :refer [context defroutes DELETE GET POST PUT routes]]
   [compojure.route :as route]
   [ring.util.response :as r]
   [camelot.util.cursorise :as cursorise]))

(defn- retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defroutes app-routes
  (context "/trap-stations" {session :session state :system}
           (GET "/site/:id" [id]
                (crud/list-resources trap-station/get-all :trap-station id (update state :config #(merge % session))))
           (GET "/survey/:id" [id]
                (crud/list-resources trap-station/get-all-for-survey :trap-station id (update state :config #(merge % session))))
           (GET "/" []
                (crud/list-resources trap-station/get-all* :trap-station (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource trap-station/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource trap-station/create!
                                                  trap-station/ttrap-station data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource trap-station/delete! id (update state :config #(merge % session)))))

  (context "/trap-station-sessions" {session :session state :system}
           (GET "/trap-station/:id" [id]
                (crud/list-resources trap-station-session/get-all :trap-station-session id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource trap-station-session/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session/delete! id (update state :config #(merge % session)))))

  (context "/trap-station-session-cameras" {session :session state :system}
           (GET "/trap-station-session/:id" [id]
                (crud/list-resources trap-station-session-camera/get-all :trap-station-session-camera id (update state :config #(merge % session))))
           (GET "/available/:id" [id] (crud/list-available trap-station-session-camera/get-available id (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-available trap-station-session-camera/get-alternatives id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource trap-station-session-camera/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session-camera/delete! id (update state :config #(merge % session))))
           (DELETE "/:id/media" [id] (crud/delete-resource
                                      trap-station-session-camera/delete-media! id (update state :config #(merge % session)))))

  (context "/sites" {session :session state :system}
           (GET "/" [] (crud/list-resources site/get-all :site (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource site/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource site/update! id
                                                       site/tsite data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource site/create!
                                                  site/tsite data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource site/delete! id (update state :config #(merge % session)))))

  (context "/cameras" {session :session state :system}
           (GET "/" [] (crud/list-resources camera/get-all :camera (update state :config #(merge % session))))
           (GET "/available" [] (crud/list-resources camera/get-available :camera (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource camera/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource camera/update! id
                                                       camera/tcamera data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource camera/create!
                                                  camera/tcamera data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource camera/delete! id (update state :config #(merge % session)))))

  (context "/surveys" {session :session state :system}
           (GET "/" [] (crud/list-resources survey/get-all :survey (update state :config #(merge % session))))
           (GET "/:id/files" [id] (crud/list-resources survey-file/get-all :survey-file id (update state :config #(merge % session))))
           (GET "/:id/files/:file-id" [id file-id] (crud/specific-resource survey-file/get-specific
                                                                           file-id (update state :config #(merge % session))))
           (GET "/:id/files/:file-id/download" [id file-id]
                (survey-file/download (update state :config #(merge % session))
                                      (edn/read-string file-id)))
           (POST "/files" {params :multipart-params}
                 (r/response (survey-file/upload! (update state :config #(merge % session))
                                                  (edn/read-string (get params "survey-id"))
                                                   (get params "file"))))
           (DELETE "/:id/files/:file-id" [id file-id] (crud/delete-resource survey-file/delete!
                                                                            file-id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource survey/get-specific id (update state :config #(merge % session))))
           (GET "/bulkimport/template" {params :params}
                (bulk-import/metadata-template (update state :config #(merge % session)) (:dir params)))
           (POST "/bulkimport/columnmap" {params :multipart-params}
                 (->> (get params "file")
                      (bulk-import/column-map-options (update state :config #(merge % session)))
                      r/response))
           (POST "/bulkimport/import" [data]
                 (r/response (bulk-import/import-with-mappings (update state :config #(merge % session)) data)))
           (PUT "/:id" [id data] (crud/update-resource survey/update! id
                                                       survey/tsurvey data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource survey/create!
                                                  survey/tsurvey data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource survey/delete! id (update state :config #(merge % session)))))

  (context "/survey-sites" {session :session state :system}
           (GET "/" [] (crud/list-resources survey-site/get-all* :survey-site (update state :config #(merge % session))))
           (GET "/survey/:id" [id] (crud/list-resources survey-site/get-all :survey-site id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource survey-site/get-specific id (update state :config #(merge % session))))
           (GET "/available/:id" [id] (crud/list-available survey-site/get-available id (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-available survey-site/get-alternatives id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource survey-site/delete! id (update state :config #(merge % session)))))

  (context "/sightings" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources sighting/get-all :sighting id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource sighting/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource sighting/update! id
                                                       sighting/tsighting data (update state :config #(merge % session))))
           (GET "/available/:id" [id] (crud/list-available sighting/get-available id (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-available sighting/get-alternatives id (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource sighting/create!
                                                  sighting/tsighting data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource sighting/delete! id (update state :config #(merge % session)))))

  (context "/camera-statuses" {session :session state :system}
           (GET "/available/" [] (crud/list-resources camera-status/get-all :camera-status (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-resources camera-status/get-all :camera-status (update state :config #(merge % session)))))

  (context "/media" {session :session state :system}
           (GET "/trap-station-session-camera/:id" [id] (crud/list-resources media/get-all :media id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource media/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource media/update! id media/tmedia data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource media/create!
                                                  media/tmedia data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource media/delete! id (update state :config #(merge % session))))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file (update state :config #(merge % session))
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file (update state :config #(merge % session))
                                                                                         filename (keyword style))}))

  (context "/photos" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources photo/get-all :photo id (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource photo/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource photo/update! id
                                                       photo/tphoto data (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource photo/create!
                                                  photo/tphoto data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource photo/delete! id (update state :config #(merge % session)))))

  (context "/taxonomy" {session :session state :system}
           (GET "/" [] (crud/list-resources taxonomy/get-all :taxonomy (update state :config #(merge % session))))
           (GET "/available/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource taxonomy/get-specific id (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data (update state :config #(merge % session))))
           (GET "/survey/:id" [id] (crud/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id (update state :config #(merge % session))))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (crud/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource ataxonomy/create!
                                                  ataxonomy/tassociated-taxonomy data (update state :config #(merge % session))))
           (DELETE "/:id" [id] (crud/delete-resource taxonomy/delete! id (update state :config #(merge % session)))))

  (context "/species-mass" {session :session state :system}
           (GET "/" [] (crud/list-resources species-mass/get-all :species-mass (update state :config #(merge % session))))
           (GET "/available/" [id] (crud/list-resources species-mass/get-all :species-mass (update state :config #(merge % session))))
           (GET "/alternatives/:id" [id] (crud/list-resources species-mass/get-all :species-mass (update state :config #(merge % session)))))

  (context "/application" {session :session state :system}
           (GET "/metadata" [] (r/response (screens/get-metadata (update state :config #(merge % session)))))
           (GET "/" [] (r/response {:version (version/get-version)
                                    :nav (screens/nav-menu (update state :config #(merge % session)))})))

  (context "/screens" {session :session state :system}
           (GET "/" [] (r/response (screens/all-screens (update state :config #(merge % session))))))

  (context "/import" {session :session state :system}
           (POST "/options" [data] (r/response (im.db/options state data)))
           (POST "/media" [data] (r/response (import/media state data))))

  (context "/report" {session :session state :system}
           (GET "/manage/rescan" [] (do (report/refresh-reports state)
                                        {:status 200
                                         :headers {"Content-Type" "text/plain; charset=utf-8"}
                                         :body "Reports rescanned."}))
           (GET "/:report/download" {params :params}
                (report/export
                 (update state :config #(merge % session))
                 (keyword (:report params)) (crud/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 (update state :config #(merge % session))
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports (update state :config #(merge % session))))))

  (context "/library" {session :session state :system}
           (GET "/" [] (r/response (library/build-library (update state :config #(merge % session)))))
           (GET "/:id" [id] (r/response (library/build-library-for-survey (update state :config #(merge % session))
                                                                          (edn/read-string id))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (update state :config #(merge % session)) data)))
           (PUT "/identify" [data] (r/response (library/identify (update state :config #(merge % session))
                                                                 data))))
  (context "/species" {session :session state :system}
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             (update state :config #(merge % session))
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               (update state :config #(merge % session))
                                               (:species data)
                                               (:survey-id data)))))

  (context "/capture" {session :session state :system}
           (POST "/upload" {params :multipart-params}
                 (r/response (capture/import-capture! (update state :config #(merge % session))
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file")))))
  (context "/deployment" {session :session state :system}
           (GET "/survey/:id" [id] (crud/list-resources deployment/get-all
                                                        :trap-station-session id (update state :config #(merge % session))))
           (POST "/create/:id" [id data] (crud/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)}) (update state :config #(merge % session))))
           (PUT "/:id" [id data] (crud/update-resource deployment/update! id
                                                       deployment/tdeployment
                                                       (assoc data :trap-station-id
                                                              {:value (edn/read-string id)}) (update state :config #(merge % session))))
           (GET "/:id" [id] (crud/specific-resource deployment/get-specific id (update state :config #(merge % session)))))
  (context "/camera-deployment" {session :session state :system}
           (GET "/survey/:id/recent" [id] (crud/list-resources camera-deployment/get-uploadable
                                                               :trap-station-session id (update state :config #(merge % session))))
           (POST "/" [data] (crud/create-resource camera-deployment/create-camera-check!
                                                  camera-deployment/tcamera-deployment data (update state :config #(merge % session)))))

  (context "/albums" {session :session state :system}
           (GET "/" [] (r/response
                        (let [s (update state :config #(merge % session))]
                          (album/read-albums s (-> s :config :root-path))))))

  (context "/settings" {session :session state :system}
           (GET "/" [] (r/response (cursorise/cursorise (merge (:config state) session))))
           (PUT "/" [data] (-> (r/response (state/save-config (cursorise/decursorise data)))
                               (assoc :session {:language (:value (:language data))}))))

  (GET "/" _ (retrieve-index))
  (POST "/quit" [] (System/exit 0))
  (GET "/quit" [] (System/exit 0))
  (route/resources "/"))
