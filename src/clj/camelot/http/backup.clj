(ns camelot.http.backup
  (:require
   [compojure.core :refer [context GET]]
   [camelot.backup.core :as backup]))

;; TODO #217 implement this
(def routes
  (context "/backup" {state :state}
           (GET "/file/:filename" [filename] (backup/download state filename))
           (GET "/manifest" [] (backup/manifest state))))
