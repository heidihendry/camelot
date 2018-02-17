(ns user
  (:require
   [camelot.util.db-migrate]
   [camelot.core :as camelot]
   [camelot.system.http.core :as http]
   [com.stuartsierra.component :as component]
   [reloaded.repl :as rrepl]
   [schema.core :as s]))

(s/set-fn-validation! true)

(defn migrate
  [state]
  (camelot.util.db-migrate/migrate (get-in state [:database :connection])))

(defn rollback
  [state]
  (camelot.util.db-migrate/rollback (get-in state [:database :connection])))

(defn start []
  (camelot/start-prod))

(defn stop []
  (swap! http/system component/stop)
  nil)

(defn restart
  []
  (stop)
  (start))

(defn state []
  @http/system)

(rrepl/set-init! camelot/start-prod)
