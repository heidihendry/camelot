(def +version+ "1.1.1-SNAPSHOT")
(def dependencies
  '[[bk/ring-gzip "0.1.1"]
    [adzerk/boot-cljs "1.7.228-2" :scope "test"]
    [adzerk/boot-reload "0.5.0" :scope "test"]
    [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
    [adzerk/boot-test "1.0.7" :scope "test"]
    [com.cemerick/piggieback "0.2.1"]
    [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
    [doo "0.1.7" :scope "test"]
    [weasel "0.7.0"]
    [cheshire "5.6.1"]
    [clj-http "2.2.0"]
    [clj-time "0.11.0"]
    [cljs-http "0.1.39"]
    [cljsjs/react-with-addons "0.14.3-0"]
    [com.andrewmcveigh/cljs-time "0.4.0"]
    [com.drewnoakes/metadata-extractor "2.9.1"]
    [com.luckycatlabs/SunriseSunsetCalculator "1.2"]
    [com.stuartsierra/component "0.3.1"]
    [com.taoensso/tower "3.1.0-beta4"]
    [commons-io/commons-io "2.4"]
    [compojure "1.5.0"]
    [environ "1.0.2"]
    [net.mikera/imagez "0.10.0"]
    [org.apache.commons/commons-lang3 "3.4"]
    [org.apache.derby/derby "10.12.1.1"]
    [org.clojure/tools.namespace "0.2.11"]
    [org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.9.229"]
    [org.clojure/core.async "0.2.391"]
    [org.clojure/data.csv "0.1.3"]
    [org.clojure/math.combinatorics "0.1.4"]
    [org.clojure/java.jdbc "0.4.2"]
    [org.clojure/tools.nrepl "0.2.12"]
    [org.omcljs/om "1.0.0-alpha32" :exclusions [com.cognitect/transit-cljs cljsjs/react]]
    [pandeiro/boot-http "0.7.6" :scope "test"]
    [prismatic/schema "1.0.5"]
    ;; Note: ragtime.jdbc/basename has been redefined in camelot.db.
    [ragtime "0.5.3"]
    [resauce "0.1.0"]
    [ring "1.4.0"]
    [ring-transit "0.1.4"]
    [ring.middleware.logger "0.5.0"]
    [ring/ring-defaults "0.2.0"]
    [secretary "1.2.3"]
    [yesql "0.5.2"]
    [figwheel "0.5.4"]
    [figwheel-sidecar "0.5.4"]])

(set-env!
 :source-paths #{"src/cljc" "src/clj" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies dependencies)

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-test :refer [test]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[ring.middleware.reload :refer [wrap-reload]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[camelot.system.http :refer [system http-handler] :as http]
         '[camelot.system.db-migrate :refer [migrate rollback]]
         '[camelot.core :as camelot]
         '[com.stuartsierra.component :as component]
         '[schema.core :as schema]
         '[figwheel-sidecar.system :as figwheel])

(task-options!
 pom {:project 'camelot
      :version +version+
      :description "Manage and analyse camera trap data. Designed for researchers and conservationists."
      :url "http://gitlab.com/camelot-project/camelot"
      :scm {:url "http://gitlab.com/camelot-project/camelot"}
      :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(def reload-http-handler
  (wrap-reload #'http-handler))

(defrecord DevHttpServer [database config]
  component/Lifecycle
  (start [this]
    (assoc this :figwheel
           (figwheel/start-figwheel!
            {:figwheel-options {:ring-handler 'boot.user/reload-http-handler
                                :css-dirs ["resources/public/css"]
                                :server-logfile "log/figwheel.log"}
             :all-builds [{:id "app"
                           :source-paths ["src/cljc" "src/cljs"]
                           :figwheel true
                           :compiler {:main 'camelot.core
                                      :asset-path "js/compiled/out"
                                      :output-to "resources/public/js/compiled/camelot.js"
                                      :output-dir "resources/public/js/compiled/out"
                                      :source-map-timestamp true}}]})))

  (stop [this]
    (if-let [fw (get this :figwheel)]
      (do
        (figwheel/stop-figwheel! fw)
        (assoc this :figwheel nil)))))

(deftask figwheel
  []
  (reset! http/system (->> {:options {:dev-server (map->DevHttpServer {})}}
                           camelot.core/camelot
                           component/start))
  identity)

(deftask dev
  "Launch Immediate Feedback Development Environment"
  [p port PORT int "The web server port to listen on (default: 3449)"]
  (let [port (or port 3449)])
  (comp
   (figwheel)
   (target :dir #{"target"})))

(deftask add-source-paths
  "Add paths to :source-paths environment variable"
  [t dirs PATH #{str} ":source-paths"]
  (merge-env! :source-paths dirs)
  identity)

(deftask check
  [n namespaces NS #{sym} "the set of namespace symbols to run tests in"]
  (let [namespaces (or namespaces #{})]
    (comp
     (add-source-paths :dirs #{"test/cljc" "test/clj" "test/cljs"})
     (test-cljs :ids ["camelot.test-runner"]
                :update-fs? true
                :namespaces namespaces
                :js-env :phantom
                :optimizations :none)
     (test :namespaces namespaces))))

(deftask build
  []
  (comp
   (aot :namespace '#{camelot.core})
   (cljs :ids #{"public/js/compiled/camelot"}
         :optimizations :advanced)
   (pom)
   (uber)
   (jar :main 'camelot.core
        :file "camelot.jar"
        :manifest {"Description" "Manage and analyse camera trap data. Designed for researchers and conservationists."
                   "Url" "http://gitlab.com/camelot-project/camelot"})
   (target :dir #{"target"})))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(schema/set-fn-validation! true)

(defn do-migrate
  [state]
  (migrate (get-in state [:database :connection])))

(defn do-rollback
  [state]
  (rollback (get-in state [:database :connection])))

(defn start-prod []
  (camelot/start-prod))

(defn start
  []
  (boot (dev)))

(defn stop []
  (swap! system component/stop)
  nil)

(defn restart
  []
  (stop)
  (start))

(defn state []
  @system)
