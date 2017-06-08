(ns camelot.model.library
  "Library models and data access."
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.util.db :as db]
   [camelot.library.filter :as filter]
   [camelot.util.filter :as futil]
   [camelot.util.datatype :as datatype]
   [camelot.model.sighting :as sighting]
   [camelot.model.media :as media]
   [camelot.system.state :refer [State]]
   [camelot.util.trap-station :as util.ts]
   [camelot.library.filter-parser :as fparser]
   [camelot.model.taxonomy :as taxonomy]
   [clojure.edn :as edn]
   [camelot.util.config :as config])
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
  [state sightings records]
  (let [md (build-library-metadata state)]
    (map #(assoc (merge (get md (:trap-station-session-camera-id %)) %)
                 :sightings (or (get sightings (:media-id %)) []))
         records)))

(defn filtered-media
  [state records search]
  (let [spps (reduce #(assoc %1 (:taxonomy-id %2) %2) {} (taxonomy/get-all state))
        sightings (group-by :media-id (sighting/get-all* state))
        matches (filter/only-matching search spps (add-in-metadata state sightings records))]
    (map #(:media-id %) matches)))

(defn all-media-with-taxonomy-label
  [state value]
  (if (= (config/lookup state :species-name-style) :scientific)
    (db/with-db-keys state -all-media-with-taxonomy-scientific-name value)
    (db/with-db-keys state -all-media-with-taxonomy-common-name value)))

(def queries
  "Query strategies for different field types."
  {:media-reference-quality
   {:query -all-media-with-reference-quality-sighting
    :check-value datatype/could-be-boolean?
    :parse-value datatype/as-boolean}

   :media-attention-needed
   {:query -all-media-with-attention-needed-flag
    :check-value datatype/could-be-boolean?
    :parse-value datatype/as-boolean}

   :media-processed
   {:query -all-media-with-media-processed-flag
    :check-value datatype/could-be-boolean?
    :parse-value datatype/as-boolean}

   :media-cameracheck
   {:query -all-media-with-media-cameracheck-flag
    :check-value datatype/could-be-boolean?
    :parse-value datatype/as-boolean}

   :trap-station-id
   {:query -all-media-with-trap-station-id
    :check-value datatype/could-be-integer?
    :parse-value edn/read-string}

   :taxonomy-id
   {:query -all-media-with-taxonomy-id
    :check-value datatype/could-be-integer?
    :parse-value edn/read-string}

   :survey-id
   {:query -all-media-with-survey-id
    :check-value datatype/could-be-integer?
    :parse-value edn/read-string}

   :camera-name {:query -all-media-with-camera-name}
   :site-name {:query -all-media-with-site-name}
   :taxonomy-label {:query all-media-with-taxonomy-label
                    :clojure-fn true}
   :taxonomy-common-name {:query -all-media-with-taxonomy-common-name}
   :taxonomy-species {:query -all-media-with-taxonomy-species}
   :taxonomy-genus {:query -all-media-with-taxonomy-genus}})

(def query-priority
  "Ordering of (likely) most to least specific field, to optimise a search."
  [:taxonomy-label :taxonomy-id :taxonomy-common-name :taxonomy-species
   :taxonomy-genus :media-reference-quality :trap-station-id
   :media-processed :media-cameracheck :media-attention-needed
   :site-name :camera-name :survey-id])

(defn query-config
  "Return a configuration, if available, to optimise a search term."
  [term]
  (let [build-config #(when %
                        [(hash-map :search (:value term)
                                   :config (get queries %))])]
    (->> query-priority
         (filter #(and (= (futil/field-key-lookup (:field term)) %)
                       (not= (:value term) "*")))
         first
         build-config)))

(defn search-valid?
  [datatype-fn value]
  (if datatype-fn
    (datatype-fn value)
    true))

(defn- execute-optimised-query-for-search
  "Return the research for a search.
  Attempts to optimise the query for simple searches."
  [state psearch]
  (if-let [qc (some->> psearch
                       first
                       (mapcat query-config)
                       first)]
    (if (search-valid? (get-in qc [:config :check-value]) (:search qc))
      (let [parse-fn (get-in qc [:config :parse-value])]
        (let [fv {:field-value (if parse-fn
                                 (parse-fn (:search qc))
                                 (:search qc))}]
          (if (get-in qc [:config :clojure-fn])
            ((get-in qc [:config :query]) state fv)
            (db/with-db-keys state (get-in qc [:config :query]) fv))))
      [])
      (db/with-db-keys state -all-media {})))

(defn execute-query-for-search
  "Executes a query for the search, returning the result."
  [state psearch]
  (if (fparser/has-disjunctions? psearch)
    (db/with-db-keys state -all-media {})
    (execute-optimised-query-for-search state psearch)))

(defn search-media
  [state search]
  (let [psearch (fparser/parse search)]
    (cond
      (fparser/match-all? psearch)
      (map :media-id (db/with-db-keys state -all-media-ids {}))

      (fparser/match-all-in-survey? psearch)
      (map :media-id (db/with-db-keys state -all-media-ids-for-survey
                       {:field-value (:value (ffirst psearch))}))

      :else
      (filtered-media state
                      (execute-query-for-search state psearch)
                      search))))

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
  [state {:keys [quantity species lifestage sex sighting-fields]} media-id]
  (media/update-processed-flag! state {:media-id media-id
                                       :media-processed true})
  (sighting/create! state (sighting/tsighting {:sighting-quantity quantity
                                               :sighting-lifestage lifestage
                                               :sighting-sex sex
                                               :taxonomy-id species
                                               :media-id media-id
                                               :sighting-fields sighting-fields})))

(s/defn identify
  "Creates identification data as sightings for each media ID given."
  [state {:keys [identification media]}]
  (let [media (media/get-with-ids state media)]
    (when (> (count (into #{} (map :survey-id) media)) 1)
      (throw (IllegalArgumentException. "Cannot identify media across multiple surveys"))))
  (db/with-transaction [s state]
    (map :sighting-id (doall (map (partial identify-media s identification) media)))))
