(ns camelot.handler.summary-statistics-report
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

(sql/defqueries "sql/summary-statistics-report.sql" {:connection db/spec})

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
       (reduce #(+ %1 (get-nights-for-group %2)) 0)))

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

(defn- get-species-photo-data
  [state sightings]
  (->> sightings
       (group-by :species-scientific-name)
       (reduce (fn [acc [k v]] (assoc acc k (count v))) {})))

(defn- report-row
  [obs locs nights photo-counts spp]
  (let [spp-obs (get obs spp)]
    (vector spp (get locs spp) (get photo-counts spp)
            spp-obs nights
            (if (= nights 0)
              "-"
              (format "%.3f"
                      (* 100 (double (/ spp-obs nights))))))))

(defn report
  [state sightings]
  (let [[obs locs nights] (get-report-aggregate-data state sightings)
        photo-counts (get-species-photo-data state sightings)]
    (->> (keys obs)
         (map (partial report-row obs locs nights photo-counts)))))

(defn csv-report
  [state sightings]
  (report-util/to-csv-string
   (cons ["Species" "Sites" "Photos" "Independent Observations" "Nights" "Observations / Night (%)"]
         (report state sightings))))

(defn export
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (get-sightings-for-survey state survey-id)
        data (csv-report state sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"summary-statistics-report.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/summary-statistics" []
           (GET "/:id" [id] (export id))))
