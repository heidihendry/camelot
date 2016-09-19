(ns user
  (:require [camelot.core]
            [camelot.migrate :refer [migrate rollback]]
            [schema.core :as s]
            [midje.repl :as midje]
            [ring.middleware.reload :refer [wrap-reload]]
            [figwheel-sidecar.repl-api :as figwheel]
            [camelot.application :as app]
            [camelot.util.config :as config]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(s/set-fn-validation! true)

(def http-handler
  (wrap-reload #'camelot.core/http-handler))

(defn run []
  (migrate)
  (figwheel/start-figwheel!))

(defn autotest []
  (midje/autotest))

(defn state []
  (app/gen-state (config/config)))

(def browser-repl figwheel/cljs-repl)

(defn config []
  (camelot.application/gen-state (camelot.util.config/config)))
