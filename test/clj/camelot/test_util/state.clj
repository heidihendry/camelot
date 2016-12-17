(ns camelot.test-util.state
  (:require
   [clj-time.coerce :as tc]
   [clj-time.core :as t]
   [camelot.system.state :as state]
   [com.stuartsierra.component :as component]))

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
  ([] {:config (component/start
                (state/map->Config {:store (atom {})
                                    :config default-config
                                    :path {}}))
       :database {:connection {}}
       :app {}})
  ([config]
   {:config (component/start
             (state/map->Config {:store (atom {})
                                 :config (merge default-config config)
                                 :path {}}))
    :database {:connection {}}
    :app {}}))
