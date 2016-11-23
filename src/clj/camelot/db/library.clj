(ns camelot.db.library
  "Library models and data access."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.db.core :as db]
   [camelot.db.sighting :as sighting]
   [camelot.db.media :as media]
   [camelot.app.state :refer [State]]
   [camelot.util.trap-station :as util.ts])
  (:import
   (camelot.db.sighting Sighting)))

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
     media-processed :- s/Bool
     media-reference-quality :- s/Bool
     media-capture-timestamp :- org.joda.time.DateTime
     trap-station-session-camera-id :- s/Int
     trap-station-session-id :- s/Int
     trap-station-id :- s/Int
     trap-station-name :- s/Str
     trap-station-longitude :- (s/pred util.ts/valid-longitude?)
     trap-station-latitude :- (s/pred util.ts/valid-latitude?)
     site-sublocation :- (s/maybe s/Str)
     site-city :- (s/maybe s/Str)
     site-state-province :- (s/maybe s/Str)
     site-country :- (s/maybe s/Str)
     camera-id :- s/Int
     camera-name :- s/Str
     camera-make :- (s/maybe s/Str)
     camera-model :- (s/maybe s/Str)
     survey-site-id :- s/Int
     survey-id :- s/Int
     survey-name :- s/Str
     site-id :- s/Int
     site-name :- s/Str
     sightings :- [Sighting]])

(s/defn library-record
  [{:keys [media-id media-created media-updated media-filename media-format
           media-uri media-cameracheck media-attention-needed media-processed
           media-reference-quality media-capture-timestamp trap-station-session-camera-id
           trap-station-session-id trap-station-id trap-station-name
           trap-station-longitude trap-station-latitude site-sublocation
           site-city site-state-province site-country camera-id camera-name
           camera-make camera-model survey-site-id survey-id survey-name
           site-id site-name sightings]}]
  (->LibraryRecord media-id media-created media-updated media-filename
                   media-format media-uri media-cameracheck
                   media-attention-needed (or media-processed false) (or media-reference-quality false)
                   media-capture-timestamp trap-station-session-camera-id
                   trap-station-session-id trap-station-id trap-station-name
                   trap-station-longitude trap-station-latitude
                   site-sublocation site-city site-state-province site-country camera-id
                   camera-name camera-make camera-model survey-site-id survey-id
                   survey-name site-id site-name (or sightings [])))

(defn- all-media
  [state]
  (db/with-db-keys state -all-media {}))

(defn- all-media-for-survey
  [state survey-id]
  (db/with-db-keys state -all-media-for-survey {:survey-id survey-id}))

(s/defn build-records :- [LibraryRecord]
  [state sightings media]
  (let [media-sightings (group-by :media-id sightings)
        media-uri #(format "/media/photo/%s" (:media-filename %))
        sightings-for #(get media-sightings (:media-id %))]
    (sort-by
     (juxt
      :trap-station-id
      :camera-id
      :trap-station-session-start-date
      :trap-station-session-id
      :media-capture-timestamp)
     (map #(library-record (assoc %
                                  :sightings (vec (sightings-for %))
                                  :media-uri (media-uri %)))
          media))))

(s/defn build-library :- [LibraryRecord]
  [state]
  (db/with-transaction [s state]
    (build-records s (sighting/get-all* s) (all-media s))))

(s/defn build-library-for-survey :- [LibraryRecord]
  [state :- State
   id :- s/Int]
  (db/with-transaction [s state]
    (build-records s (sighting/get-all* s) (all-media-for-survey s id))))

(s/defn update-bulk-media-flags
  [state :- State
   data]
  (db/with-transaction [s state]
    (doall (map (partial media/update-media-flags! s) data))))

(defn- identify-media
  [state {:keys [quantity species lifestage sex]} media-id]
  (media/update-processed-flag! state {:media-id media-id
                                       :media-processed true})
  (sighting/create! state (sighting/tsighting {:sighting-quantity quantity
                                               :sighting-lifestage lifestage
                                               :sighting-sex sex
                                               :taxonomy-id species
                                               :media-id media-id})))

(s/defn identify
  [state {:keys [identification media]}]
  (db/with-transaction [s state]
    (map :sighting-id (doall (map (partial identify-media s identification) media)))))
