(ns camelot.system.migrater
  (:require
   [camelot.system.protocols :as protocols]
   [camelot.util.maintenance :as maintenance]
   [com.stuartsierra.component :as component]))

;; TODO #217 most of the stuff in c.u.maintenance today should live here
(defrecord Migrater []
  protocols/Migratable
  (migrate [this dataset]
    (maintenance/safe-migrate! dataset))

  (rollback [this dataset]
    (maintenance/rollback dataset))

  component/Lifecycle
  (start [this] this)
  (stop [this] this))
