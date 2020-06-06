(ns camelot.core
  "Camelot - Camera Trap management software for conservation research."
  (:require
   [com.stuartsierra.component :as component]
   [camelot.system.core :as system])
  (:gen-class))

(defn start-prod
  []
  (component/start (system/camelot-system {})))

(defn -main [& _]
  (start-prod))
