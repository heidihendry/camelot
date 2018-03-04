(ns camelot.model.library.core
  "Library models and data access."
  (:require
   [schema.core :as s]
   [medley.core :as medley]
   [camelot.util.db :as db]
   [clojure.tools.logging :as log]
   [camelot.model.library.filter :as filter]
   [camelot.report.query :as query]
   [camelot.util.filter :as futil]
   [camelot.util.datatype :as datatype]
   [camelot.model.sighting :as sighting]
   [camelot.model.media :as media]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.trap-station :as util.ts]
   [camelot.model.library.filter-parser :as fparser]
   [camelot.model.taxonomy :as taxonomy]
   [clojure.edn :as edn]
   [camelot.util.config :as config])
  (:import
   (camelot.model.sighting Sighting)))

(def query (db/with-db-keys :library))

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
  (->> (query state :hierarchy-data {})
       (group-by :trap-station-session-camera-id)
       (map (fn [[k v]] (vector k (map->LibraryMetadata (first v)))))
       (into {})))

(defn search-media
  [state search]
  (let [psearch (fparser/parse search)]
    (cond
      (fparser/match-all? psearch)
      (map :media-id (query state :all-media-ids {}))

      (fparser/match-all-in-survey? psearch)
      (map :media-id (query state :all-media-ids-for-survey
                       {:field-value (:value (ffirst psearch))}))

      :else
      (->> (query/get-by state :media)
           (filter/only-matching psearch)
           (sort-by (juxt :trap-station-id
                          :camera-id
                          :trap-station-session-start-date
                          :trap-station-session-id
                          :media-capture-timestamp))
           (map :media-id)
           distinct))))

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
  [state {:keys [quantity species sighting-fields]} media-id]
  (media/update-processed-flag! state {:media-id media-id
                                       :media-processed true})
  (sighting/create! state (sighting/tsighting {:sighting-quantity quantity
                                               :taxonomy-id species
                                               :media-id media-id
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