(ns dev
  (:require
   [camelot.core]
   [camelot.util.state :as state]
   [camelot.system.state :as sysstate]
   [camelot.system.systems :as systems]
   [camelot.util.maintenance :as maintenance]
   [camelot.system.http.core :as http]
   [com.stuartsierra.component :as component]
   [ring.middleware.reload :refer [wrap-reload]]
   [schema.core :as s]
   [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(s/set-fn-validation! true)

(def http-handler
  (wrap-reload #'http/http-handler))

;; TODO these are no longer so convenient.  May want to add dataset-id param?
(defn migrate
  [state]
  (maintenance/migrate state))

(defn rollback
  [state]
  (maintenance/rollback state))

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
  (reset! sysstate/system (-> (state/system-config)
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
