(ns camelot.system.config.core
  (:require
   [camelot.util.state :as state]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

(defrecord Config [config]
  component/Lifecycle
  (start [this]
    (merge this (state/system-config)))

  (stop [this]
    (log/info "Config stopped.")
    this))
