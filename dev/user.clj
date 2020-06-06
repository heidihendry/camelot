(ns user
  (:require
   [camelot.core]
   [camelot.system.core :as system]
   [camelot.util.maintenance :as maintenance]
   [camelot.system.http.core :as http]
   [com.stuartsierra.component :as component]
   [ring.middleware.reload :refer [wrap-reload]]
   [schema.core :as s]
   [figwheel-sidecar.repl-api :as figwheel]
   [reloaded.repl :refer [go start stop reset system]]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(s/set-fn-validation! true)

(def http-handler nil)

(defrecord DevHttpServer [database config]
  component/Lifecycle
  (start [this]
    (if (:figwheel this)
      this
      (do
        (let [hf (http/http-handler this)]
          (alter-var-root #'http-handler (constantly hf)))
        (figwheel/start-figwheel!)
        (assoc this :figwheel true))))

  (stop [this]
    (when (get this :figwheel)
      (figwheel/stop-figwheel!)
      (assoc this :figwheel nil))))

(reloaded.repl/set-init! #(system/camelot-system {:app (map->DevHttpServer {})}))

(defn migrate
  [dataset]
  (maintenance/migrate dataset))

(defn rollback
  [dataset]
  (maintenance/rollback dataset))

(defn runprod []
  (camelot.core/start-prod))
