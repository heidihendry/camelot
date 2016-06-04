(ns camelot.handler.survey-site-report
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

(sql/defqueries "sql/reports.sql" {:connection db/spec})

(defn- get-sightings-for-survey-site
  [state id]
  (db/with-db-keys state -get-sightings-for-survey-site {:survey-site-id id}))

(defn- get-all-species
  [state]
  (db/clj-keys (-get-all-species)))

(defn- species-sighting-reducer
  [acc v]
  (let [spp (:species v)
        qty (:count v)]
    (assoc acc spp (+ (or (get acc spp) 0) qty))))

(defn- get-independent-sightings
  [state sightings]
  (->> sightings
       (group-by :trap-station-session-id)
       (vals)
       (map #(remove (fn [x] (nil? (:species-scientific-name x))) %))
       (map (partial album/extract-independent-sightings state))
       (flatten)
       (reduce species-sighting-reducer {})))

(defn- get-nights-for-group
  [group]
  (let [start (:trap-station-session-start-date (first group))
        end (:trap-station-session-end-date (first group))]
    (t/in-days (t/interval start end))))

(defn- get-species-in-group
  [group]
  (distinct (map :species-scientific-name group)))

(defn- get-species-nights
  [grouped-sightings]
  (->> grouped-sightings
       (group-by :trap-station-session-id)
       (vals)
       (reduce #(+ %1 (get-nights-for-group %2)) 0)))

(defn- inc-or-one
  [v]
  (if (nil? v)
    1
    (inc v)))

(defn- get-report-aggregate-data
  [state sightings]
  (->> sightings
       ((juxt (partial get-independent-sightings state)
              get-species-nights))))

(defn- report-row
  [obs nights spp]
  (let [spp-obs (get obs spp)]
    (vector spp
            (if (or (nil? spp-obs) (zero? spp-obs)) "" "X")
            (or spp-obs "0")
            nights
            (if (= nights 0)
              "-"
              (format "%.3f"
                      (* 100 (double (/ (or spp-obs 0) nights))))))))

(defn report
  [state species sightings]
  (let [[obs nights] (get-report-aggregate-data state sightings)]
    (->> species
         (map (partial report-row obs nights)))))

(defn csv-report
  [state species sightings]
  (report-util/to-csv-string
   (cons ["Species" "Presence" "Independent Observations" "Nights" "Observations / Night (%)"]
         (report state species sightings))))

(defn export
  [survey-id]
  (let [state (app/gen-state (config/config))
        sightings (get-sightings-for-survey-site state survey-id)
        species (map :species-scientific-name (get-all-species state))
        data (csv-report state species sightings)]
    (-> (r/response data)
        (r/content-type "text/csv; charset=utf-8")
        (r/header "Content-Length" (count data))
        (r/header "Content-Disposition" "attachment; filename=\"survey-site-statistics.csv\""))))

(def routes
  "Species summary report routes."
  (context "/report/survey-site-statistics" []
           (GET "/:id" [id] (export id))))
