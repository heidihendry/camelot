(ns user
  (:require
   [camelot.core]
   [camelot.system.http :as http]
   [com.stuartsierra.component :as component]
   [reloaded.repl :as rrepl]
   [schema.core :as s]))

(s/set-fn-validation! true)

(defn migrate
  [state]
  (camelot.system.db-migrate/migrate (get-in state [:database :connection])))

(defn rollback
  [state]
  (camelot.system.db-migrate/rollback (get-in state [:database :connection])))

(defn runprod []
  (camelot.core/start-prod))

(defn start []
  (reset! http/system (->> {}
                           camelot.core/camelot
                           component/start)))

(defn stop []
  (swap! http/system component/stop)
  nil)

(defn restart
  []
  (stop)
  (start))

(defn state []
  @http/system)

(rrepl/set-init! camelot.core/start-prod)
