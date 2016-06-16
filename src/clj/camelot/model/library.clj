(ns camelot.model.library
  (:require  [schema.core :as s]
             [yesql.core :as sql]
             [camelot.db :as db]
             [camelot.model.sighting :as sighting]
             [camelot.model.media :as media]
             [camelot.model.state :refer [State]]
             [camelot.model.trap-station :as trap-station])
  (:import [camelot.model.sighting Sighting]))

(sql/defqueries "sql/library.sql" {:connection db/spec})

(s/defrecord LibraryRecord
    [media-id :- s/Int
     media-created :- org.joda.time.DateTime
     media-updated :- org.joda.time.DateTime
     media-filename :- s/Str
     media-format :- s/Str
     media-uri :- s/Str
     media-cameracheck :- s/Bool
     media-attention-needed :- s/Bool
     media-capture-timestamp :- org.joda.time.DateTime
     trap-station-session-camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-id :- s/Int
     trap-station-name :- s/Str
     trap-station-longitude :- (s/pred trap-station/valid-longitude?)
     trap-station-latitude :- (s/pred trap-station/valid-latitude?)
     site-sublocation :- (s/maybe s/Str)
     site-city :- (s/maybe s/Str)
     camera-id :- s/Int
     camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     survey-site-id :- s/Int
     survey-id :- s/Int
     site-id :- s/Int
     site-name :- s/Str
     sightings :- [Sighting]])

(s/defn library-record
  [{:keys [media-id media-created media-updated media-filename media-format media-uri media-cameracheck media-attention-needed
           media-capture-timestamp trap-station-session-camera-id trap-station-session-id trap-station-id
           trap-station-name trap-station-longitude trap-station-latitude site-sublocation site-city camera-id
           camera-name camera-make camera-model survey-site-id survey-id site-id site-name sightings]}]
  (->LibraryRecord media-id media-created media-updated media-filename media-format media-uri media-cameracheck media-attention-needed
                   media-capture-timestamp trap-station-session-camera-id trap-station-session-id trap-station-id
                   trap-station-name trap-station-longitude trap-station-latitude site-sublocation site-city camera-id
                   camera-name camera-make camera-model survey-site-id survey-id site-id site-name (or sightings [])))

(defn- all-media
  [state]
  (db/with-db-keys state -all-media {}))

(s/defn build-records :- [LibraryRecord]
  [state sightings media]
  (let [media-sightings (group-by :media-id sightings)
        media-uri #(format "/media/photo/%s" (:media-filename %))
        sightings-for #(get media-sightings (:media-id %))]
    (map #(library-record (assoc %
                                 :sightings (vec (sightings-for %))
                                 :media-uri (media-uri %)))
         media)))

(s/defn build-library :- [LibraryRecord]
  [state]
  (db/with-transaction [s state]
    (build-records s (sighting/get-all* s) (all-media s))))

(s/defn update-bulk-media-flags
  [state :- State
   data]
  (db/with-transaction [s state]
    (doall (map (partial media/update-media-flags s) data))))

(defn- identify-media
  [state {:keys [quantity species]} media-id]
  (sighting/create! state (sighting/tsighting {:sighting-quantity quantity
                                               :species-id species
                                               :media-id media-id})))

(s/defn identify
  [state {:keys [identification media]}]
  (db/with-transaction [s state]
    (map :sighting-id (doall (map (partial identify-media s identification) media)))))
