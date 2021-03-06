(ns camelot.model.suggestion
  "Suggestion models and data access."
  (:require
   [camelot.util.data :as data-util]
   [camelot.util.db :as db]))

(def query (db/with-db-keys :suggestion))

(defrecord TSuggestion
    [suggestion-key
     suggestion-label
     suggestion-confidence
     bounding-box-id
     media-id])

(defrecord Suggestion
    [suggestion-id
     suggestion-created
     suggestion-updated
     suggestion-key
     suggestion-label
     suggestion-confidence
     bounding-box
     media-id])

(defn suggestion
  [data]
  (-> data
      (data-util/key-prefix-to-map [:bounding-box])
      (data-util/dissoc-if :bounding-box #(nil? (-> % :bounding-box :id)))
      (map->Suggestion)
      ;; TODO Enrich suggestion data from module
      (assoc :suggestion-data {})))

(def tsuggestion map->TSuggestion)

(defn get-all
  "Retrieve all suggestions for the given `media-id`."
  [state media-id]
  (->> {:media-id media-id}
       (query state :get-all)
       (map suggestion)))

(defn get-all*
  "Retrieve all suggestions."
  [state]
  (->> {}
       (query state :get-all*)
       (map suggestion)))

(defn get-all-for-media-ids
  "Retrieve all suggestions for the given collection of `media-ids`."
  [state media-ids]
  (->> {:media-ids media-ids}
       (query state :get-all-for-media-ids)
       (filter #(>= (:suggestion-confidence %)
                    (get-in state [:config :detector :confidence-threshold])))
       (map suggestion)))

(defn get-specific
  "Retrieve a suggestion with the given `id`."
  [state id]
  (some->> {:suggestion-id id}
           (query state :get-specific)
           first
           suggestion))

(defn create!
  "Create a suggestion with the given data."
  [state data]
  (let [record (query state :create<! data)
        suggestion-id (int (:1 record))]
    (get-specific state suggestion-id)))

(defn delete!
  "Delete a suggestion with the given `id`."
  [state id]
  ;; TODO Delete suggestion data from module
  ;; TODO Delete dangling bounding boxes
  (query state :delete! {:suggestion-id id}))

(defn delete-for-media-id!
  "Delete all suggestions for `media-idid`."
  [state media-id]
  (query state :delete-for-media-id! {:media-id media-id}))

(defn delete-with-bounding-box!
  "Delete a suggestion with the given `bounding-box-id`."
  [state bounding-box-id]
  ;; TODO Delete suggestion data from module
  (query state :delete-with-bounding-box! {:bounding-box-id bounding-box-id}))
