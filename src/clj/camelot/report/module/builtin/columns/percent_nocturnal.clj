(ns camelot.report.module.builtin.columns.percent-nocturnal
  (:require [camelot.util.sunrise-sunset :as sun]
            [camelot.report.module.core :as module]
            [clj-time.core :as t]
            [camelot.report.module.column-util :as col-util]))

(defn- is-night?
  [{:keys [trap-station-latitude
           trap-station-longitude
           media-capture-timestamp]}]
  (if (nil? media-capture-timestamp)
    nil
    (let [sunrise (sun/get-sunrise-time
                   (str trap-station-latitude)
                   (str trap-station-longitude)
                   media-capture-timestamp)
          sunset (sun/get-sunset-time
                  (str trap-station-latitude)
                  (str trap-station-longitude)
                  media-capture-timestamp)]
      (or (t/before? media-capture-timestamp sunrise)
          (t/after? media-capture-timestamp sunset)
          (= media-capture-timestamp sunset)))))

(defn calculate-is-night
  [state data]
  (map #(assoc % :percent-nocturnal (case (is-night? %)
                                      true "X"
                                      false ""
                                      nil))
       data))

(defn aggregate-is-night
  [col data]
  (col-util/aggregate-boolean :media-id col data))

(module/register-column
 :percent-nocturnal
 {:calculate calculate-is-night
  :aggregate aggregate-is-night})
