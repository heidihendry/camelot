(ns camelot.library.core
  "Library models and data access."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.model.sighting :as sighting]
   [camelot.model.suggestion :as suggestion]
   [camelot.model.media :as media]
   [camelot.spec.schema.state :refer [State]]
   [clojure.edn :as edn]
   [camelot.library.query.core :as query])
  (:import
   (camelot.model.sighting Sighting)))

(def sqlquery (db/with-db-keys :library))

(defrecord LibraryMetadata
    [trap-station-session-camera-id
     trap-station-session-id
     trap-station-id
     trap-station-name
     trap-station-longitude
     trap-station-latitude
     site-sublocation
     site-city
     site-state-province
     site-country
     camera-id
     camera-name
     camera-make
     camera-model
     survey-site-id
     survey-id
     survey-name
     site-id
     site-name])

(defn build-library-metadata
  [state]
  (->> (sqlquery state :hierarchy-data {})
       (group-by :trap-station-session-camera-id)
       (map (fn [[k v]] (vector k (map->LibraryMetadata (first v)))))
       (into {})))

(defn query-media
  [state qstr]
  (query/query-media state qstr))

(s/defn build-records
  [state sightings suggestions media]
  (let [media-sightings (group-by :media-id sightings)
        media-suggestions (group-by :media-id suggestions)
        media-uri #(format "/media/photo/%s" (:media-filename %))
        sightings-for #(get media-sightings (:media-id %))
        suggestions-for #(get media-suggestions (:media-id %))]
    (map #(assoc %
                 :sightings (vec (sightings-for %))
                 :suggestions (if (:media-processed %)
                                []
                                (vec (suggestions-for %)))
                 :media-uri (media-uri %))
         media)))

(s/defn hydrate-media
  [state ids]
  (if (seq ids)
    (let [media (media/get-list state ids)
          sightings (sighting/get-all-for-media-ids state ids)
          suggestions (suggestion/get-all-for-media-ids state ids)]
      (build-records state sightings suggestions media))
    []))

(s/defn update-bulk-media-flags
  [state :- State
   data]
  (db/with-transaction [s state]
    (doall (map (partial media/update-media-flags! s) data))))

(defn- identify-media
  [state {:keys [quantity species sighting-fields bounding-box-id]} media-id]
  (media/update-processed-flag! state {:media-id media-id
                                       :media-processed true})
  (sighting/create! state (sighting/tsighting {:sighting-quantity quantity
                                               :taxonomy-id species
                                               :media-id media-id
                                               :bounding-box-id bounding-box-id
                                               :sighting-fields (reduce-kv #(assoc %1 %2 (str %3))
                                                                           {} sighting-fields)})))

(s/defn identify
  "Creates identification data as sightings for each media ID given."
  [state {:keys [identification media]}]
  (let [media (media/get-with-ids state media)]
    (when (> (count (into #{} (map :survey-id) media)) 1)
      (throw (IllegalArgumentException. "Cannot identify media across multiple surveys"))))
  (db/with-transaction [s state]
    (map :sighting-id (doall (map (partial identify-media s identification) media)))))

(s/defn update-identification!
  "Update sighting information for the given sighting ID."
  [state id {:keys [quantity species sighting-fields]}]
  (sighting/update! state (edn/read-string id)
                    (sighting/tsighting-update
                     {:sighting-quantity quantity
                      :taxonomy-id species
                      :sighting-fields (reduce-kv #(assoc %1 %2 (str %3))
                                                  {} sighting-fields)})))
