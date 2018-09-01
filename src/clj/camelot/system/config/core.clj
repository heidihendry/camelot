(ns camelot.system.config.core
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]))

(defrecord Config [store config path]
  component/Lifecycle
  (start [this]
    (reset! store config)
    this)

  (stop [this]
    (when store
      (reset! store {}))
    (log/info "Config stopped.")
    (assoc this
           :store nil
           :config nil
           :path nil)))
