(ns camelot.library.query.honeysql
  (:require [honeysql.format :as fmt]
            [honeysql.core :as honeysql]
            [honeysql.helpers :as honeyhelpers]))

(defmethod fmt/fn-handler "concat" [_ a b]
  (str "(" (fmt/to-sql a) " || " (fmt/to-sql b) ")"))

(defmethod fmt/fn-handler "like" [_ a b]
  (str (fmt/to-sql-value a) " LIKE " (fmt/to-sql-value b)))

(defmethod fmt/fn-handler "not like" [_ a b]
  (str (fmt/to-sql-value a) " NOT LIKE " (fmt/to-sql-value b)))

(defn base-query
  [state]
  {:select [:trap-station.trap-station-id
            :camera.camera-id
            :trap-station-session.trap-station-session-start-date
            :trap-station-session.trap-station-session-id
            :media.media-capture-timestamp
            :media.media-id]
   :modifiers [:distinct]
   :from [:media]
   :left-join [:trap-station-session-camera
               [:= :trap-station-session-camera.trap-station-session-camera-id
                :media.trap-station-session-camera-id]

               :trap-station-session
               [:= :trap-station-session.trap-station-session-id
                :trap-station-session-camera.trap-station-session-id]

               :trap-station
               [:= :trap-station.trap-station-id
                :trap-station-session.trap-station-id]

               :survey-site
               [:= :survey-site.survey-site-id
                :trap-station.survey-site-id]

               :survey
               [:= :survey.survey-id :survey-site.survey-id]

               :site
               [:= :site.site-id
                :survey-site.site-id]

               :camera
               [:= :camera.camera-id
                :trap-station-session-camera.camera-id]

               :camera-status
               [:= :camera-status.camera-status-id :camera.camera-status-id]

               :sighting
               [:= :sighting.media-id :media.media-id]

               :suggestion
               [:and [:= :suggestion.media-id :media.media-id]
                [:>= :suggestion.suggestion-confidence (-> state :config :detector :confidence-threshold)]]

               :taxonomy
               [:= :taxonomy.taxonomy-id :sighting.taxonomy-id]

               :species-mass
               [:= :species-mass.species-mass-id :taxonomy.species-mass-id]

               :sighting-field
               [:= :sighting-field.survey-id :survey.survey-id]

               :sighting-field-value
               [:and [:= :sighting-field-value.sighting-field-id :sighting-field.sighting-field-id]
                [:= :sighting-field-value.sighting-id :sighting.sighting-id]]

               :photo [:= :photo.media-id :media.media-id]]
   :order-by [:trap-station.trap-station-id
              :camera.camera-id
              :trap-station-session.trap-station-session-start-date
              :trap-station-session.trap-station-session-id
              :media.media-capture-timestamp
              :media.media-id]})

(defn finalise-query
  [query]
  (honeysql/format {:select [:result.media_id]
                    :from [[query :result]]}))

(defn build-query
  [state pt]
  (-> (base-query state)
      (honeyhelpers/where pt)))
