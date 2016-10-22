(ns camelot.report.module.builtin.columns.percent-nocturnal
  (:require [camelot.util.sunrise-sunset :as sun]
            [camelot.report.module.core :as module]
            [clj-time.core :as t]
            [camelot.report.module.column-util :as col-util]
            [camelot.report.sighting-independence :as indep])
  (:import [java.util TimeZone]))

(defn- get-timezone
  [state]
  (let [tz-str (get-in state [:config :timezone])]
    (if tz-str
      (TimeZone/getTimeZone ^String tz-str)
      (TimeZone/getDefault))))

(defn- is-night?
  [state
   {:keys [trap-station-latitude
           trap-station-longitude
           media-capture-timestamp]}]
  (let [tz (get-timezone state)]
    (if (or (nil? media-capture-timestamp)
            (nil? trap-station-longitude)
            (nil? trap-station-latitude))
      nil
      (let [sunrise (sun/get-sunrise-time tz
                     (str trap-station-latitude)
                     (str trap-station-longitude)
                     media-capture-timestamp)
            sunset (sun/get-sunset-time tz
                    (str trap-station-latitude)
                    (str trap-station-longitude)
                    media-capture-timestamp)]
        (or (t/before? media-capture-timestamp sunrise)
            (t/after? media-capture-timestamp sunset)
            (= media-capture-timestamp sunset))))))

(defn calculate-is-night
  [state data]
  (map #(assoc % :percent-nocturnal (case (is-night? state %)
                                      true "X"
                                      false ""
                                      nil))
       data))

(defn aggregate-is-night
  [state col data]
  (col-util/aggregate-boolean-by-independent-observations
   :media-id state col (indep/->independent-sightings state data)))

(module/register-column
 :percent-nocturnal
 {:calculate calculate-is-night
  :aggregate aggregate-is-night})
