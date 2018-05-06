(ns camelot.http.backup
  (:require
   [compojure.core :refer [context GET]]
   [camelot.backup.core :as backup]))

(def routes
  (context "/backup" {session :session state :system}
           (GET "/file/:filename" [filename] (backup/download state filename))
           (GET "/manifest" [] (backup/manifest state))))




