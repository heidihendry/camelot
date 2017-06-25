(ns camelot.system.routes
  (:require
   [camelot.system.screens :as screens]
   [camelot.system.state :as state]
   [camelot.system.version :as version]
   [camelot.system.db-migrate :as db-migrate]
   [camelot.import.capture :as capture]
   [camelot.import.bulk :as bulk]
   [camelot.import.template :as template]
   [camelot.services.species-search :as species-search]
   [camelot.model.associated-taxonomy :as ataxonomy]
   [camelot.model.camera :as camera]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.sighting :as sighting]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.model.site :as site]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.model.species-mass :as species-mass]
   [camelot.model.survey :as survey]
   [camelot.model.survey-site :as survey-site]
   [camelot.model.survey-file :as survey-file]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.trap-station :as trap-station]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.model.deployment :as deployment]
   [camelot.model.camera-deployment :as camera-deployment]
   [camelot.model.library :as library]
   [camelot.report.core :as report]
   [camelot.system.crud-util :as crud]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [compojure.core :refer [context defroutes DELETE GET POST PUT routes]]
   [compojure.route :as route]
   [ring.util.response :as r]
   [camelot.util.cursorise :as cursorise]
   [camelot.system.importer :as importer]))

(defn- retrieve-index
  "Return a response for index.html"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "www/index.html"))})

(defn- heartbeat
  [state]
  (let [conn (get-in state [:database :connection])]
    {:status 200
     :headers {"Content-Type" "text/plain; charset=utf-8"}
     :body (format "Status: OK\nSoftware version: %s\nDatabase version: %s\n"
                   (version/get-version)
                   (db-migrate/version conn))}))

(defroutes app-routes
  (context "/trap-stations" {session :session state :system}
           (GET "/site/:id" [id]
                (crud/list-resources trap-station/get-all :trap-station id (assoc state :session session)))
           (GET "/survey/:id" [id]
                (crud/list-resources trap-station/get-all-for-survey :trap-station id (assoc state :session session)))
           (GET "/" []
                (crud/list-resources trap-station/get-all* :trap-station (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station/update! id
                                                       trap-station/ttrap-station data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station/create!
                                                  trap-station/ttrap-station data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station/delete! id (assoc state :session session))))

  (context "/trap-station-sessions" {session :session state :system}
           (GET "/trap-station/:id" [id]
                (crud/list-resources trap-station-session/get-all :trap-station-session id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station-session/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session/update! id
                                                       trap-station-session/ttrap-station-session data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station-session/create!
                                                  trap-station-session/ttrap-station-session data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session/delete! id (assoc state :session session))))

  (context "/trap-station-session-cameras" {session :session state :system}
           (GET "/trap-station-session/:id" [id]
                (crud/list-resources trap-station-session-camera/get-all :trap-station-session-camera id (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available trap-station-session-camera/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available trap-station-session-camera/get-alternatives id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource trap-station-session-camera/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource trap-station-session-camera/update! id
                                                       trap-station-session-camera/ttrap-station-session-camera data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource trap-station-session-camera/create!
                                                  trap-station-session-camera/ttrap-station-session-camera data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource trap-station-session-camera/delete! id (assoc state :session session)))
           (DELETE "/:id/media" [id] (crud/delete-resource
                                      trap-station-session-camera/delete-media! id (assoc state :session session))))

  (context "/sites" {session :session state :system}
           (GET "/" [] (crud/list-resources site/get-all :site (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource site/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource site/update! id
                                                       site/tsite data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource site/create!
                                                  site/tsite data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource site/delete! id (assoc state :session session))))

  (context "/cameras" {session :session state :system}
           (GET "/" [] (crud/list-resources camera/get-all :camera (assoc state :session session)))
           (GET "/available" [] (crud/list-resources camera/get-available :camera (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource camera/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource camera/update! id
                                                       camera/tcamera data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource camera/create!
                                                  camera/tcamera data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource camera/delete! id (assoc state :session session))))

  (context "/surveys" {session :session state :system}
           (GET "/" [] (crud/list-resources survey/get-all :survey (assoc state :session session)))
           (GET "/:id/files" [id] (crud/list-resources survey-file/get-all :survey-file id (assoc state :session session)))
           (GET "/:id/files/:file-id" [id file-id] (crud/specific-resource survey-file/get-specific
                                                                           file-id (assoc state :session session)))
           (GET "/:id/files/:file-id/download" [id file-id]
                (survey-file/download (assoc state :session session)
                                      (edn/read-string file-id)))
           (POST "/files" {params :multipart-params}
                 (r/response (survey-file/upload! (assoc state :session session)
                                                  (edn/read-string (get params "survey-id"))
                                                   (get params "file"))))
           (DELETE "/:id/files/:file-id" [id file-id] (crud/delete-resource survey-file/delete!
                                                                            file-id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource survey/get-specific id (assoc state :session session)))
           (GET "/bulkimport/template" {params :params}
                (template/metadata-template (assoc state :session session) (:dir params)))
           (POST "/bulkimport/columnmap" {params :multipart-params}
                 (->> (get params "file")
                      (template/column-map-options (assoc state :session session))
                      r/response))
           (POST "/bulkimport/import" [data]
                 (r/response (bulk/import-with-mappings (assoc state :session session) data)))
           (PUT "/:id" [id data] (crud/update-resource survey/update! id
                                                       survey/tsurvey data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource survey/create!
                                                  survey/tsurvey data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource survey/delete! id (assoc state :session session))))

  (context "/survey-sites" {session :session state :system}
           (GET "/" [] (crud/list-resources survey-site/get-all* :survey-site (assoc state :session session)))
           (GET "/survey/:id" [id] (crud/list-resources survey-site/get-all :survey-site id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource survey-site/get-specific id (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available survey-site/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available survey-site/get-alternatives id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource survey-site/update! id
                                                       survey-site/tsurvey-site data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource survey-site/create!
                                                  survey-site/tsurvey-site data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource survey-site/delete! id (assoc state :session session))))

  (context "/sightings" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources sighting/get-all :sighting id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource sighting/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource sighting/update! id
                                                       sighting/tsighting data (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-available sighting/get-available id (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-available sighting/get-alternatives id (assoc state :session session)))
           (POST "/" [data] (crud/create-resource sighting/create!
                                                  sighting/tsighting data (assoc state :session session)))
           (DELETE "/media" [data] (r/response (sighting/delete-with-media-ids! (assoc state :session session)
                                                                                (:media-ids data))))
           (DELETE "/:id" [id] (crud/delete-resource sighting/delete! id (assoc state :session session))))

  (context "/sighting-field-values" {session :session state :system}
           (GET "/" [] (r/response (vals (sighting-field-value/query-all (assoc state :session session))))))

  (context "/camera-statuses" {session :session state :system}
           (GET "/available/" [] (crud/list-resources camera-status/get-all :camera-status (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources camera-status/get-all :camera-status (assoc state :session session))))

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
                                                                                         filename (keyword style))}))

  (context "/photos" {session :session state :system}
           (GET "/media/:id" [id] (crud/list-resources photo/get-all :photo id (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource photo/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource photo/update! id
                                                       photo/tphoto data (assoc state :session session)))
           (POST "/" [data] (crud/create-resource photo/create!
                                                  photo/tphoto data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource photo/delete! id (assoc state :session session))))

  (context "/taxonomy" {session :session state :system}
           (GET "/" [] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/available/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources taxonomy/get-all :taxonomy (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource taxonomy/get-specific id (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource taxonomy/update! id
                                                       taxonomy/ttaxonomy data (assoc state :session session)))
           (GET "/survey/:id" [id] (crud/list-resources taxonomy/get-all-for-survey
                                                        :taxonomy id (assoc state :session session)))
           (DELETE "/:taxonomy-id/survey/:survey-id" [taxonomy-id survey-id]
                   (crud/delete-resource taxonomy/delete-from-survey!
                                         {:survey-id survey-id
                                          :taxonomy-id taxonomy-id} (assoc state :session session)))
           (POST "/" [data] (crud/create-resource ataxonomy/create!
                                                  ataxonomy/tassociated-taxonomy data (assoc state :session session)))
           (DELETE "/:id" [id] (crud/delete-resource taxonomy/delete! id (assoc state :session session))))

  (context "/species-mass" {session :session state :system}
           (GET "/" [] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session)))
           (GET "/available/" [id] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session)))
           (GET "/alternatives/:id" [id] (crud/list-resources species-mass/get-all :species-mass (assoc state :session session))))

  (context "/application" {session :session state :system}
           (GET "/" [] (r/response {:version (version/get-version)
                                    :nav (screens/nav-menu (assoc state :session session))})))

  (context "/screens" {session :session state :system}
           (GET "/" [] (r/response (screens/all-screens (assoc state :session session)))))

  (context "/report" {session :session state :system}
           (GET "/manage/rescan" [] (do (report/refresh-reports state)
                                        {:status 200
                                         :headers {"Content-Type" "text/plain; charset=utf-8"}
                                         :body "Reports rescanned."}))
           (GET "/:report/download" {params :params}
                (report/export
                 (assoc state :session session)
                 (keyword (:report params)) (crud/coerce-string-fields params)))
           (GET "/:report" [report] (r/response (report/get-configuration
                                                 (assoc state :session session)
                                                 (keyword report))))
           (GET "/" [] (r/response (report/available-reports (assoc state :session session)))))

  (context "/library" {session :session state :system}
           (POST "/" [data]
                 (r/response (library/search-media (assoc state :session session)
                                                    (:search data))))
           (GET "/metadata" [] (r/response (library/build-library-metadata
                                            (assoc state :session session))))
           (POST "/hydrate" [data]
                 (r/response (library/hydrate-media (assoc state :session session)
                                                    (:media-ids data))))
           (POST "/media/flags" [data] (r/response (library/update-bulk-media-flags
                                                    (assoc state :session session)
                                                    data)))
           (PUT "/identify" [data] (r/response (library/identify (assoc state :session session)
                                                                 data))))
  (context "/species" {session :session state :system}
           (GET "/search" {{search :search} :params}
                (r/response (species-search/query-search
                             (assoc state :session session)
                             {:search search})))
           (POST "/create" [data] (r/response (species-search/ensure-survey-species-known
                                               (assoc state :session session)
                                               (:species data)
                                               (:survey-id data)))))

  (context "/capture" {session :session state :system}
           (POST "/upload" {params :multipart-params}
                 (r/response (capture/import-capture! (assoc state :session session)
                                                      (edn/read-string (get params "session-camera-id"))
                                                      (get params "file")))))
  (context "/deployment" {session :session state :system}
           (GET "/survey/:id" [id] (crud/list-resources deployment/get-all
                                                        :trap-station-session id (assoc state :session session)))
           (POST "/create/:id" [id data] (crud/create-resource deployment/create!
                                                               deployment/tdeployment
                                                               (assoc data :survey-id
                                                                      {:value (edn/read-string id)}) (assoc state :session session)))
           (PUT "/:id" [id data] (crud/update-resource deployment/update! id
                                                       deployment/tdeployment
                                                       (assoc data :trap-station-id
                                                              {:value (edn/read-string id)}) (assoc state :session session)))
           (GET "/:id" [id] (crud/specific-resource deployment/get-specific id (assoc state :session session))))
  (context "/camera-deployment" {session :session state :system}
           (GET "/survey/:id/recent" [id] (crud/list-resources camera-deployment/get-uploadable
                                                               :trap-station-session id (assoc state :session session)))
           (POST "/" [data] (crud/create-resource camera-deployment/create-camera-check!
                                                  camera-deployment/tcamera-deployment data (assoc state :session session))))

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
                                                     (assoc state :session session))))

  (context "/settings" {session :session state :system}
           (GET "/" [] (r/response (cursorise/cursorise (merge (deref (get-in state [:config :store])) session))))
           (PUT "/" [data] (assoc (r/response (state/save-config (cursorise/decursorise data)))
                                  :session {:language (:value (:language data))})))

  (context "/importer" {session :session state :system}
           (GET "/" [] (r/response (importer/importer-state state)))
           (POST "/cancel" [] (r/response (importer/cancel-import state))))

  (GET "/" _ (retrieve-index))
  (GET "/heartbeat" {session :session state :system}
       (heartbeat state))
  (POST "/quit" [] (System/exit 0))
  (GET "/quit" [] (System/exit 0))
  (route/resources "/" {:root "www"}))
