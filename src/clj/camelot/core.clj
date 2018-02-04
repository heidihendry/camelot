(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require
   [camelot.system.core :as system]
   [camelot.system.cli :as cli]
   [environ.core :refer [env]]
   [clojure.tools.nrepl.server :as nrepl]
   [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))

(defn start-prod
  ([]
   (start-prod {}))
  ([opts]
   (system/begin opts)
   (system/user-mode!)))

(defn -main [& args]
  (start-prod (:options (parse-opts args cli/cli-opts))))
