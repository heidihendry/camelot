(ns camelot.testutil.state
  (:require
   [camelot.testutil.mock :as mock]
   [clj-time.coerce :as tc]
   [clj-time.core :as t]
   [camelot.system.config :as config]
   [com.stuartsierra.component :as component]
   [clojure.java.io :as io]))

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

(def default-datasets
  {:default {:paths {:media (io/file "/path/to/media")
                     :database (io/file "/path/to/media")
                     :filestore-base (io/file "/path/to/filestore")
                     :backup (io/file "/path/to/backup")}}})

(defn gen-state
  ([] {:config default-config
       :database {}
       :datasets (mock/datasets default-datasets :default)
       :app {:port 5341 :browser false}
       :session {:dataset-id :default}})
  ([config]
   {:config (merge default-config config)
    :database {}
    :datasets (mock/datasets default-datasets :default)
    :app {:port 5341 :browser false}
    :session {:dataset-id :default}})
  ([config queries]
   {:config (merge default-config config)
    :database {:queries queries}
    :datasets (mock/datasets default-datasets :default)
    :app {:port 5341 :browser false}
    :session {:dataset-id :default}}))
