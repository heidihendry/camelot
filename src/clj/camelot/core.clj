(ns camelot.core
  "Camelot - Camera Trap management software for conservation research."
  (:require
   [camelot.util.state :as state]
   [camelot.system.core :as system]
   [environ.core :refer [env]]
   [clojure.tools.nrepl.server :as nrepl]
   [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn start-prod
  ([]
   (start-prod (state/read-config)))
  ([config]
   (system/begin config)
   (system/user-mode!)))

(defn -main [& args]
  (let [config (state/read-config)]
    (if-let [port (:debugger-port config)]
      (nrepl/start-server :port port))
    (start-prod config)))
