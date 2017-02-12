(ns camelot.model.library
  "Library models and data access."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]
   [camelot.library.filter :as filter]
   [camelot.model.sighting :as sighting]
   [camelot.model.media :as media]
   [camelot.system.state :refer [State]]
   [camelot.util.trap-station :as util.ts]
   [camelot.model.taxonomy :as taxonomy])
  (:import
   (camelot.model.sighting Sighting)))

(sql/defqueries "sql/library.sql")

(def max-result-records 2000)

(defrecord LibraryRecord
    [media-id
     media-created
     media-updated
     media-filename
     media-format
     media-uri
     media-cameracheck
     media-attention-needed
     media-processed
     media-reference-quality
     media-capture-timestamp
     trap-station-session-camera-id
     sightings])

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

(defn library-record
  [ks]
  (map->LibraryRecord (-> ks
                          (update :media-processed #(or % false))
                          (update :media-reference-quality #(or % false))
                          (update :sightings #(or % [])))))

(defn build-library-metadata
  [state]
  (->> (db/with-db-keys state -hierarchy-data {})
       (group-by :trap-station-session-camera-id)
       (map (fn [[k v]] (vector k (map->LibraryMetadata (first v)))))
       (into {})))

(defn add-in-metadata
  [state records]
  (let [md (build-library-metadata state)]
    (map #(merge (get md (:trap-station-session-camera-id %)) %)
         records)))

(defn filtered-media
  [state records {:keys [search]}]
  (let [spps (reduce #(assoc %1 (:taxonomy-id %2) %2) {} (taxonomy/get-all state))
        matches (filter/only-matching search spps (add-in-metadata state records))]
    (map #(:media-id %) matches)))

(defn search-media
  ([state search]
   (filtered-media state
                   (db/with-db-keys state -all-media {})
                   {:search search}))
  ([state survey-id search]
   (filtered-media state
                   (db/with-db-keys state -all-media-for-survey {:survey-id survey-id})
                   {:search search})))

(s/defn build-records
  [state sightings media]
  (let [media-sightings (group-by :media-id sightings)
        media-uri #(format "/media/photo/%s" (:media-filename %))
        sightings-for #(get media-sightings (:media-id %))]
    (map #(assoc %
                 :sightings (vec (sightings-for %))
                 :media-uri (media-uri %))
         media)))

(s/defn hydrate-media
  [state ids]
  (->> ids
       (map #(media/get-specific state %))
       (build-records state (sighting/get-all* state))))

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
