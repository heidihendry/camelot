(ns camelot.report.module.builtin.columns.percent-nocturnal
  "Column and aggregation for whether a indep. obs. is nocturnal."
  (:require
   [camelot.util.sunrise-sunset :as sun]
   [camelot.report.module.core :as module]
   [clj-time.core :as t]
   [camelot.report.module.column-util :as col-util]
   [camelot.report.sighting-independence :as indep]
   [camelot.util.config :as config])
  (:import
   (java.util TimeZone)))

(defn- get-timezone
  "Return a timezone from the configuration, or the default (system) timezone if unavailable."
  [state]
  (let [tz-str (config/lookup state :timezone)]
    (if tz-str
      (TimeZone/getTimeZone ^String tz-str)
      (TimeZone/getDefault))))

(defn- is-night?
  "Predicate indicating whether the record capture time is at night for the given location."
  [state
   {lat :trap-station-latitude
    lon :trap-station-longitude
    ts :media-capture-timestamp}]
  (when-not (some nil? [ts lat lon])
    (let [rise-set-fn (juxt sun/get-sunrise-time sun/get-sunset-time)
          [sunrise sunset]
          (rise-set-fn (get-timezone state) (str lat) (str lon) ts)]
      (or (t/before? ts sunrise)
          (t/after? ts sunset)
          (t/equal? ts sunset)))))

(defn calculate-is-night
  "Assoc percent-noctural flag, indicating with 'X' if at night."
  [state data]
  (map #(assoc % :percent-nocturnal (case (is-night? state %)
                                      true "X"
                                      false ""
                                      nil))
       data))

(defn aggregate-is-night
  "Assoc percent-nocturnal to the percentage of flagged indep. sightings."
  [state col data-group]
  (col-util/aggregate-boolean-by-independent-observations
   state col (indep/->independent-sightings state data-group)))

(module/register-column
 :percent-nocturnal
 {:calculate calculate-is-night
  :aggregate aggregate-is-night})
