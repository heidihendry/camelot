(ns user
  (:require
   [camelot.core]
   [camelot.util.state :as state]
   [camelot.system.state :as sysstate]
   [camelot.system.systems :as systems]
   [camelot.util.db-migrate :as db-migrate]
   [camelot.system.http.core :as http]
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(s/set-fn-validation! true)

(defn migrate
  [state]
  (db-migrate/migrate (get-in state [:database :connection])))

(defn rollback
  [state]
  (db-migrate/rollback (get-in state [:database :connection])))

(defn runprod []
  (camelot.core/start-prod))

(defrecord DevHttpServer [database config]
  component/Lifecycle
  (start [this]
    (figwheel/start-figwheel!)
    (assoc this :figwheel true))

  (stop [this]
    (when (get this :figwheel)
      (figwheel/stop-figwheel!)
      (assoc this :figwheel nil))))

(defn start []
  (reset! sysstate/system (-> (state/read-config)
                              (assoc-in [:server :dev-server]
                                        (map->DevHttpServer {}))
                           systems/camelot-system
                           component/start))
  nil)

(defn stop []
  (swap! sysstate/system component/stop)
  nil)

(defn restart
  []
  (stop)
  (start))

(defn state []
  @sysstate/system)
