(ns user
  (:require
   [camelot.core]
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
  (wrap-reload #'camelot.app.http/http-handler))

(defn migrate
  [state]
  (camelot.app.db-migrate/migrate (get-in state [:database :connection])))

(defn run []
  (camelot.core/start))

(defn runprod []
  (camelot.core/start-prod))

(defn stop []
  (camelot.core/stop))

(def browser-repl figwheel/cljs-repl)
