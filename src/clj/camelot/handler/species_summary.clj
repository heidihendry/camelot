(ns camelot.handler.species-summary
  (:require [camelot.db :as db]
            [yesql.core :as sql]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [camelot.handler.albums :as albums]
            [camelot.processing.album :as album]
            [camelot.util.application :as app]
            [camelot.util.config :as config]
            [clj-time.core :as t]
            [camelot.util.report :as report-util]
            [ring.util.response :as r]))

(sql/defqueries "sql/species-summary-report.sql" {:connection db/spec})

(defn- get-sightings-for-survey
  [state id]
  (db/with-db-keys state -get-sightings-for-survey {:survey-id id}))

(defn- species-sighting-reducer
  [acc v]
  (let [spp (:species v)
        qty (:count v)]
    (assoc acc spp (+ (or (get acc spp) 0) qty))))

(defn- get-independent-sightings
  [state grouped-sightings]
  (->> grouped-sightings
       (map (partial album/extract-independent-sightings state))
       (flatten)
       (reduce species-sighting-reducer {})))

(defn- get-nights-for-group
  [group]
  (prn group)
  (let [start (:trap-station-session-start-date (first group))
        end (:trap-station-session-end-date (first group))]
    (t/in-days (t/interval start end))))

(defn- species-nights-reducer
  [acc [spps n]]
  (reduce #(update %1 %2 (fn [v] (+ n (or v 0))))
          acc spps))

(defn- get-species-in-group
  [group]
  (distinct (map :species-scientific-name group)))

(defn- get-species-nights
  [grouped-sightings]
  (->> grouped-sightings
       (flatten)
       (group-by :trap-station-session-id)
       (vals)
       (map #((juxt get-species-in-group
                    get-nights-for-group)
              %))
       (reduce species-nights-reducer {})))

(defn- inc-or-one
  [v]
  (if (nil? v)
    1
    (inc v)))

(defn- get-species-locations
  [grouped-sightings]
  (->> grouped-sightings
       (map get-species-in-group)
       (flatten)
       (reduce #(update %1 %2 inc-or-one) {})))

(defn- get-report-aggregate-data
  [state sightings]
  (->> sightings
       (group-by :trap-station-id)
       (vals)
       ((juxt (partial get-independent-sightings state)
              get-species-locations
              get-species-nights))))

(defn- report-row
  [obs locs nights spp]
  (let [spp-obs (get obs spp)
        spp-nights (get nights spp)]
    (vector spp (get locs spp) spp-obs spp-nights
            (if (= spp-nights 0)
              "-"
              (format "%.5f"
                      (double (/ spp-obs spp-nights)))))))

(defn report
  [state sightings]
  (let [[obs locs nights] (get-report-aggregate-data state sightings)]
    (->> (keys obs)
         (map (partial report-row obs locs nights)))))

(defn csv-report
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (get-sightings-for-survey state survey-id)]
    (report-util/to-csv-string
     (cons ["Species" "Locations" "Independent Observations" "Nights" "Observations / Night"]
           (build-report state sightings)))))

(defn export
  [survey-id]
  (let [data (csv-report survey-id)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"species-summary.csv\""))))

(def routes
  "Species summary report routes."
  (context "/species-summary" []
           (GET "/:id" [id] (export id))))
