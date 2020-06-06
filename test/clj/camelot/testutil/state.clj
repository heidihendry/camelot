(ns camelot.testutil.state
  (:require
   [clj-time.coerce :as tc]
   [clj-time.core :as t]
   [camelot.system.config :as config]
   [com.stuartsierra.component :as component]))

(def default-config
  "Return the default configuration."
  {:language :en
   :send-usage-data false
   :server {:http-port 5341}
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-latitude]
                     [:datetime] [:filename]]
   :paths {:root "/path/to/root"}
   :datasets {:default {}}})

(defn gen-state
  ([] {:config (component/start
                (config/map->Config default-config))
       :database {}
       :app {:port 5341 :browser false}
       :session {:dataset-id :default}})
  ([config]
   {:config (component/start
             (config/map->Config (merge default-config config)))
    :database {}
    :app {:port 5341 :browser false}
    :session {:dataset-id :default}})
  ([config queries]
   {:config (component/start
             (config/map->Config (merge default-config config)))
    :database {:queries queries}
    :app {:port 5341 :browser false}
    :session {:dataset-id :default}}))
