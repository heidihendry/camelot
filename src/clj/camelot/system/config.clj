(ns camelot.system.config
  (:refer-clojure :exclude [get])
  (:require
   [camelot.system.protocols :as protocols]
   [camelot.market.config :as market-config]
   [clojure.java.io :as io]
   [camelot.util.data :as datautil]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

(defn- paths-to-file-objects
  "Transform all values under :paths to `File` objects."
  [m]
  (update m :paths #(datautil/update-vals % io/file)))

(defrecord Config [config]
  protocols/Configurable
  (config [this]
    (-> (market-config/read-config)
      (paths-to-file-objects)
      (update :datasets datautil/update-vals paths-to-file-objects)))

  component/Lifecycle
  (start [this]
    (merge this (.config this)))

  (stop [this]
    (log/info "Config stopped.")
    this))
