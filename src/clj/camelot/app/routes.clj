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
  (context "/trap-stations" {session :session}
           (GET "/site/:id" [id]
                (crud/list-resources trap-station/get-all :trap-station id session))
           (GET "/survey/:id" [id]
                (crud/list-resources trap-station/get-all-for-survey :trap-station id session))
           (GET "/" []
                (crud/list-resources trap-station/get-all* :trap-station session))
           (GET "/:id" [id] (crud/specific-resource trap-station/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data session))
           (POST "/" [data] (crud/create-resource trap-station/create!
                                                  trap-station/ttrap-station data session))
           (DELETE "/:id" [id] (crud/delete-resource trap-station/delete! id session)))

  (context "/trap-station-sessions" {session :session}
           (GET "/trap-station/:id" [id]
                (crud/list-resources trap-station-session/get-all :trap-station-session id session))
           (GET "/:id" [id] (crud/specific-resource trap-station-session/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data session))
           (POST "/" [data] (crud/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data session))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session/delete! id session)))

  (context "/trap-station-session-cameras" {session :session}
           (GET "/trap-station-session/:id" [id]
                (crud/list-resources trap-station-session-camera/get-all :trap-station-session-camera id session))
           (GET "/available/:id" [id] (crud/list-available trap-station-session-camera/get-available id session))
           (GET "/alternatives/:id" [id] (crud/list-available trap-station-session-camera/get-alternatives id session))
           (GET "/:id" [id] (crud/specific-resource trap-station-session-camera/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data session))
           (POST "/" [data] (crud/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data session))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session-camera/delete! id session))
           (DELETE "/:id/media" [id] (crud/delete-resource
                                      trap-station-session-camera/delete-media! id session)))

  (context "/sites" {session :session}
           (GET "/" [] (crud/list-resources site/get-all :site session))
           (GET "/:id" [id] (crud/specific-resource site/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource site/update! id
                                                       site/tsite data session))
           (POST "/" [data] (crud/create-resource site/create!
                                                  site/tsite data session))
           (DELETE "/:id" [id] (crud/delete-resource site/delete! id session)))

  (context "/cameras" {session :session}
           (GET "/" [] (crud/list-resources camera/get-all :camera session))
           (GET "/available" [] (crud/list-resources camera/get-available :camera session))
           (GET "/:id" [id] (crud/specific-resource camera/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource camera/update! id
                                                       camera/tcamera data session))
           (POST "/" [data] (crud/create-resource camera/create!
                                                  camera/tcamera data session))
           (DELETE "/:id" [id] (crud/delete-resource camera/delete! id session)))

  (context "/surveys" {session :session}
           (GET "/" [] (crud/list-resources survey/get-all :survey session))
           (GET "/:id/files" [id] (crud/list-resources survey-file/get-all :survey-file id session))
           (GET "/:id/files/:file-id" [id file-id] (crud/specific-resource survey-file/get-specific
                                                                           file-id session))
           (GET "/:id/files/:file-id/download" [id file-id]
                (survey-file/download (state/gen-state session)
                                      (edn/read-string file-id)))
           (POST "/files" {params :multipart-params}
                 (r/response (survey-file/upload! (state/gen-state session)
                                                  (edn/read-string (get params "survey-id"))
                                                   (get params "file"))))
           (DELETE "/:id/files/:file-id" [id file-id] (crud/delete-resource survey-file/delete!
                                                                            file-id session))
           (GET "/:id" [id] (crud/specific-resource survey/get-specific id session))
           (GET "/bulkimport/template" {params :params}
                (bulk-import/metadata-template (state/gen-state session) (:dir params)))
           (POST "/bulkimport/columnmap" {params :multipart-params}
                 (->> (get params "file")
                      (bulk-import/column-map-options (state/gen-state session))
                      r/response))
           (POST "/bulkimport/import" [data]
                 (r/response (bulk-import/import-with-mappings (state/gen-state session) data)))
           (PUT "/:id" [id data] (crud/update-resource survey/update! id
                                                       survey/tsurvey data session))
           (POST "/" [data] (crud/create-resource survey/create!
                                                  survey/tsurvey data session))
           (DELETE "/:id" [id] (crud/delete-resource survey/delete! id session)))

  (context "/survey-sites" {session :session}
           (GET "/" [] (crud/list-resources survey-site/get-all* :survey-site session))
           (GET "/survey/:id" [id] (crud/list-resources survey-site/get-all :survey-site id session))
           (GET "/:id" [id] (crud/specific-resource survey-site/get-specific id session))
           (GET "/available/:id" [id] (crud/list-available survey-site/get-available id session))
           (GET "/alternatives/:id" [id] (crud/list-available survey-site/get-alternatives id session))
           (PUT "/:id" [id data] (crud/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data session))
           (POST "/" [data] (crud/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data session))
           (DELETE "/:id" [id] (crud/delete-resource survey-site/delete! id session)))

  (context "/sightings" {session :session}
           (GET "/media/:id" [id] (crud/list-resources sighting/get-all :sighting id session))
           (GET "/:id" [id] (crud/specific-resource sighting/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource sighting/update! id
                                                       sighting/tsighting data session))
           (GET "/available/:id" [id] (crud/list-available sighting/get-available id session))
           (GET "/alternatives/:id" [id] (crud/list-available sighting/get-alternatives id session))
           (POST "/" [data] (crud/create-resource sighting/create!
                                                  sighting/tsighting data session))
           (DELETE "/:id" [id] (crud/delete-resource sighting/delete! id session)))

  (context "/camera-statuses" {session :session}
           (GET "/available/" [] (crud/list-resources camera-status/get-all :camera-status session))
           (GET "/alternatives/:id" [id] (crud/list-resources camera-status/get-all :camera-status session)))

  (context "/media" {session :session}
           (GET "/trap-station-session-camera/:id" [id] (crud/list-resources media/get-all :media id session))
           (GET "/:id" [id] (crud/specific-resource media/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource media/update! id media/tmedia data session))
           (POST "/" [data] (crud/create-resource media/create!
                                                  media/tmedia data session))
           (DELETE "/:id" [id] (crud/delete-resource media/delete! id session))
           (GET "/photo/:filename" [filename] (let [style :original]
                                                {:status 200
                                                 :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                 :body (media/read-media-file (state/gen-state session)
                                                                              filename (keyword style))}))
           (GET "/photo/:filename/:style" [filename style] {:status 200
                                                            :headers {"Content-Type" "image/jpeg; charset=utf-8"}
                                                            :body (media/read-media-file (state/gen-state session)
                                                                                         filename (keyword style))}))

  (context "/photos" {session :session}
           (GET "/media/:id" [id] (crud/list-resources photo/get-all :photo id session))
           (GET "/:id" [id] (crud/specific-resource photo/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource photo/update! id
                                                       photo/tphoto data session))
           (POST "/" [data] (crud/create-resource photo/create!
                                                  photo/tphoto data session))
           (DELETE "/:id" [id] (crud/delete-resource photo/delete! id session)))

  (context "/taxonomy" {session :session}
           (GET "/" [] (crud/list-resources taxonomy/get-all :taxonomy session))
           (GET "/available/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy session))
           (GET "/alternatives/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy session))
           (GET "/:id" [id] (crud/specific-resource taxonomy/get-specific id session))
           (PUT "/:id" [id data] (crud/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data session))
           (GET "/survey/:id" [id] (crud/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id session))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (crud/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} session))
           (POST "/" [data] (crud/create-resource ataxonomy/create!
                                                  ataxonomy/tassociated-taxonomy data session))
           (DELETE "/:id" [id] (crud/delete-resource taxonomy/delete! id session)))

  (context "/species-mass" {session :session}
           (GET "/" [] (crud/list-resources species-mass/get-all :species-mass session))
           (GET "/available/" [id] (crud/list-resources species-mass/get-all :species-mass session))
           (GET "/alternatives/:id" [id] (crud/list-resources species-mass/get-all :species-mass session)))

  (context "/application" {session :session}
           (GET "/metadata" [] (r/response (screens/get-metadata (state/gen-state session))))
           (GET "/" [] (r/response {:version (version/get-version)
                                    :nav (screens/nav-menu (state/gen-state session))})))

  (context "/screens" {session :session}
           (GET "/" [] (r/response (screens/all-screens (state/gen-state session)))))

  (context "/import" {session :session}
           (POST "/options" [data] (r/response (im.db/options data)))
           (POST "/media" [data] (r/response (import/media data))))

  (context "/report" {session :session}
           (GET "/:report/download" {params :params}
                (report/export
                 (state/gen-state session)
                 (keyword (:report params)) (crud/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 (state/gen-state session)
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports (state/gen-state session)))))

  (context "/library" {session :session}
           (GET "/" [] (r/response (library/build-library (state/gen-state session))))
           (GET "/:id" [id] (r/response (library/build-library-for-survey (state/gen-state session)
                                                                          (edn/read-string id))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (state/gen-state session) data)))
           (PUT "/identify" [data] (r/response (library/identify (state/gen-state session)
                                                                 data))))
  (context "/species" {session :session}
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             (state/gen-state session)
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               (state/gen-state session)
                                               (:species data)
                                               (:survey-id data)))))

  (context "/capture" {session :session}
           (POST "/upload" {params :multipart-params}
                 (r/response (capture/import-capture! (state/gen-state session)
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file")))))
  (context "/deployment" {session :session}
           (GET "/survey/:id" [id] (crud/list-resources deployment/get-all
                                                        :trap-station-session id session))
           (POST "/create/:id" [id data] (crud/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)}) session))
           (PUT "/:id" [id data] (crud/update-resource deployment/update! id
                                                       deployment/tdeployment
                                                       (assoc data :trap-station-id
                                                              {:value (edn/read-string id)}) session))
           (GET "/:id" [id] (crud/specific-resource deployment/get-specific id session)))
  (context "/camera-deployment" {session :session}
           (GET "/survey/:id/recent" [id] (crud/list-resources camera-deployment/get-uploadable
                                                               :trap-station-session id session))
           (POST "/" [data] (crud/create-resource camera-deployment/create-camera-check!
                                                  camera-deployment/tcamera-deployment data session)))

  (context "/albums" {session :session}
           (GET "/" [] (r/response
                        (let [s (state/gen-state session)]
                          (album/read-albums s (-> s :config :root-path))))))

  (context "/settings" {session :session}
           (GET "/" [] (r/response (cursorise/cursorise (state/config session))))
           (PUT "/" [data] (-> (r/response (state/save-config (cursorise/decursorise data)))
                               (assoc :session {:language (:value (:language data))}))))

  (GET "/" _ (retrieve-index))
  (POST "/quit" [] (System/exit 0))
  (GET "/quit" [] (System/exit 0))
  (route/resources "/"))
