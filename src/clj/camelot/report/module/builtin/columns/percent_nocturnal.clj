(ns camelot.report.module.builtin.columns.percent-nocturnal
  "Column and aggregation for whether a indep. obs. is nocturnal."
  (:require
   [camelot.util.sunrise-sunset :as sun]
   [camelot.report.module.core :as module]
   [clj-time.core :as t]
   [camelot.state.config :as config]
   [camelot.report.module.column-util :as col-util]
   [camelot.report.sighting-independence :as indep]
   [camelot.model.survey :as survey])
  (:import
   (org.joda.time DateTime)
   (java.util TimeZone)))

(defn- get-timezone
  "Return a timezone from the configuration, or the default (system) timezone if unavailable."
  [state]
  (let [tz-str (config/lookup (:config state) :timezone)]
    (if tz-str
      (TimeZone/getTimeZone ^String tz-str)
      (TimeZone/getDefault))))

(defn maybe-night-in-extreme-latitude?
  "Heuristic for checking night if we don't have a sunrise or sunset."
  [ts lat]
  (let [apr-sep? (some? (some #{(.getMonthOfYear ^DateTime ts)} (range 4 10)))]
    (if (pos? ^long lat)
      (not apr-sep?)
      apr-sep?)))

(defn- is-night?
  "Predicate indicating whether the record capture time is at night for the given location."
  [state
   {lat :trap-station-latitude
    lon :trap-station-longitude
    ts :media-capture-timestamp}]
  (when-not (some nil? [ts lat lon])
    (let [rise-set-fn (juxt sun/sunrise-time sun/sunset-time)
          [sunrise sunset] (rise-set-fn (get-timezone state) (str lat) (str lon) ts)]
      (or
       (and sunrise (t/before? ts sunrise))
       (and sunset (t/after? ts sunset))
       (and sunset (t/equal? ts sunset))
       (and (nil? sunrise) (nil? sunset) (maybe-night-in-extreme-latitude? ts lat))
       false))))

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
  (survey/with-survey-settings [s state]
    (col-util/aggregate-boolean-by-independent-observations
     state col (indep/->independent-sightings s data-group))))

(module/register-column
 :percent-nocturnal
 {:calculate calculate-is-night
  :aggregate aggregate-is-night})
