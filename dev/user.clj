(ns user
  (:require
   [camelot.core]
   [camelot.system.http :as http]
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [ring.middleware.reload :refer [wrap-reload]]
   [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(s/set-fn-validation! true)

(def http-handler
  (wrap-reload #'camelot.system.http/http-handler))

(defn migrate
  [state]
  (camelot.system.db-migrate/migrate (get-in state [:database :connection])))

(defn rollback
  [state]
  (camelot.system.db-migrate/rollback (get-in state [:database :connection])))

(defn runprod []
  (camelot.core/start-prod))

(def browser-repl figwheel/cljs-repl)

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
  (reset! http/system (->> {:options {:dev-server (map->DevHttpServer {})}}
                           camelot.core/camelot
                           component/start))
  nil)

(defn stop []
  (swap! http/system component/stop)
  nil)

(defn restart
  []
  (stop)
  (start))

(defn state []
  @http/system)
