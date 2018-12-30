(ns camelot.system.config.core
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

(defrecord Config [config]
  component/Lifecycle
  (start [this]
    (merge this config))

  (stop [this]
    (log/info "Config stopped.")
    this))
