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
  (wrap-reload #'camelot.core/http-handler))

(defn migrate
  []
  (camelot.db.migrate/migrate))

(defn run []
  (camelot.db.migrate/migrate)
  (figwheel/start-figwheel!))

(def browser-repl figwheel/cljs-repl)

(defn state []
  (camelot.app.state/gen-state (camelot.util.config/config)))
