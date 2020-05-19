(ns camelot.core
  "Camelot - Camera Trap management software for conservation research."
  (:require
   [camelot.system.core :as system])
  (:gen-class))

(defn start-prod
  []
  (system/camelot {}))

(defn -main [& _]
  (start-prod))
