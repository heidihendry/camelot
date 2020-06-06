(ns camelot.system.config
  (:require
   [camelot.state.config :as config]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

(defrecord Config [config]
  component/Lifecycle
  (start [this]
    (merge this (config/read-config)))

  (stop [this]
    (log/info "Config stopped.")
    this))
