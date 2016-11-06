(ns camelot.test-util.state
  (:require
   [camelot.application :as app]
   [clj-time.coerce :as tc]
   [clj-time.core :as t]))

(def default-config
  "Return the default configuration."
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :root-path "/path/to/root"
   :night-end-hour 5
   :send-usage-data false
   :night-start-hour 21
   :project-start (t/date-time 2012 12 12 12 12 12)
   :project-end (t/date-time 2015 3 15 15 15 15)
   :sighting-independence-minutes-threshold 20
   :surveyed-species []
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-latitude]
                     [:datetime] [:filename]]})

(defn gen-state
  ([] {:config default-config})
  ([config]
   {:config (merge default-config config)}))
