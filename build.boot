(def +version+ "1.3.3-SNAPSHOT")

(def dependencies
  '[[adzerk/boot-cljs "1.7.228-2" :scope "test"]
    [adzerk/boot-reload "0.5.0" :scope "test"]
    [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
    [adzerk/boot-test "1.0.7" :scope "test"]
    [samestep/boot-refresh "0.1.0" :scope "test"]
    [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
    [com.cemerick/piggieback "0.2.1" :scope "test"]
    [weasel "0.7.0" :scope "test"]
    [reloaded.repl "0.2.3" :scope "test"]
    [org.apache.derby/derbytools "10.12.1.1" :scope "test"]

    [org.clojure/clojure "1.8.0"]
    [org.clojure/clojurescript "1.9.671"]
    [org.clojure/core.async "0.2.391"]
    [org.clojure/data.csv "0.1.3"]
    [org.clojure/java.jdbc "0.4.2"]
    [org.clojure/tools.nrepl "0.2.12"]
    [org.clojure/math.combinatorics "0.1.4"]
    [org.clojure/tools.namespace "0.2.11"]
    [org.clojure/tools.cli "0.3.5"]

    [com.cognitect/transit-clj "0.8.300"]
    [org.apache.derby/derby "10.12.1.1"]
    [org.omcljs/om "1.0.0-alpha32"
     :exclusions [com.cognitect/transit-clj
                  com.cognitect/transit-cljs
                  cljsjs/react]]
    [com.stuartsierra/component "0.3.1"]
    [compojure "1.5.0"]
    [ragtime "0.5.3"]
    [resauce "0.1.0"]
    [yesql "0.5.2"]
    [riddley "0.1.4"]
    [ring.middleware.logger "0.5.0"]
    [ring/ring-defaults "0.2.0"]
    [ring "1.4.0"]
    [ring-transit "0.1.4" :exclusions [com.cognitect/transit-clj]]
    [secretary "1.2.3"]
    [com.taoensso/tower "3.1.0-beta4"]

    [medley "1.0.0"]
    [bk/ring-gzip "0.1.1"]
    [cheshire "5.6.1"]
    [clj-http "2.2.0"]
    [clj-time "0.11.0"]
    [cljs-http "0.1.39" :exclusions [com.cognitect/transit-clj]]
    [cljsjs/react-with-addons "0.14.3-0"]
    [com.andrewmcveigh/cljs-time "0.5.0"]
    [com.drewnoakes/metadata-extractor "2.9.1"]
    [com.luckycatlabs/SunriseSunsetCalculator "1.2"]
    [commons-io/commons-io "2.4"]
    [environ "1.0.2"]
    [net.mikera/imagez "0.10.0"]
    [org.apache.commons/commons-lang3 "3.4"]
    [prismatic/schema "1.1.6"]])

(set-env!
 :source-paths #{"src/cljc" "src/clj" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies dependencies)

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-test :refer [test]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[samestep.boot-refresh :refer [refresh]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[clojure.tools.namespace.repl :as ns.repl]
         '[com.stuartsierra.component :as component]
         '[schema.core :as schema])

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(require '[camelot.core :as camelot]
         '[camelot.system.http :refer [system http-handler] :as http]
         '[camelot.system.db-migrate :refer [migrate rollback]])

(def project "camelot")
(def repl-port 5600)

(task-options!
 pom {:project (symbol project)
      :version +version+
      :description "Manage and analyse camera trap data. Designed for researchers and conservationists."
      :url "http://gitlab.com/camelot-project/camelot"
      :scm {:url "http://gitlab.com/camelot-project/camelot"}
      :license {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}}
 cljs {:ids #{"www/js/compiled/camelot"}
       :optimizations :advanced}
 cljs-repl {:nrepl-opts {:client false
                         :port repl-port
                         :init-ns 'user}}
 aot {:namespace #{'camelot.core}}
 jar {:main 'camelot.core
      :file (str project ".jar")
      :manifest {"Description" "Manage and analyse camera trap data. Designed for researchers and conservationists."
                 "Url" "http://gitlab.com/camelot-project/camelot"}})

(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (let [run? (atom false)]
    (with-pass-thru _
      (when-not @run?
        (reset! run? true)
        (require 'reloaded.repl)
        (let [go (resolve 'reloaded.repl/go)]
          (try
            (require 'user)
            (go)
            (catch Exception e
              (boot.util/fail "Exception while starting the system\n")
              (boot.util/print-ex (.getCause e)))))))))

(deftask dev
  "Start a development environment."
  [p port PORT int "The web server port to listen on"]
  (set-env! :source-paths #(conj % "dev"))
  (System/setProperty "camelot.version" +version+)
  (apply ns.repl/set-refresh-dirs (get-env :directories))
  (comp
   (watch)
   (reload :ids #{"www/js/compiled/camelot"}
           :asset-path "/www")
   (cljs-repl)
   (cljs :optimizations :none)
   (dev-system)
   (target)))

(deftask add-source-paths
  "Add paths to :source-paths environment variable"
  [t dirs PATH #{str} ":source-paths"]
  (merge-env! :source-paths dirs)
  identity)

(deftask test-all
  "Run tests for all source paths."
  [n namespaces NS #{sym} "the set of namespace symbols to run tests in"]
  (let [namespaces (or namespaces #{})]
    (comp
     (add-source-paths :dirs #{"test/cljc" "test/clj" "test/cljs"})
     (test :namespaces namespaces)
     (test-cljs :ids ["camelot/test-runner"]
                :namespaces namespaces
                :cljs-opts {:foreign-libs
                            [{:provides ["cljsjs.react"]
                              :file "www/lib/react-with-addons-0.14.3.js"
                              :file-min "www/lib/react-with-addons-0.14.3.js"}]}
                :js-env :phantom))))

(deftask uberjar
  "Build an uberjar."
  []
  (comp
   (aot)
   (cljs)
   (pom)
   (uber)
   (jar)
   (target :dir #{"target"})))

(defonce dev-watcher (atom nil))

(defn watcher-running?
  []
  (and (future? @dev-watcher)
       (not (future-cancelled? @dev-watcher))
       (not (future-done? @dev-watcher))))

(defn start []
  (if (watcher-running?)
    (println "Already running. Run '(stop)' first.")
    (do
      (println "Starting dev server in the background.")
      (reset! dev-watcher (future (boot (dev))))
      nil)))

(defn stop []
  (if (watcher-running?)
    (do
      (println "Stopping dev server...")
      (swap! system component/stop)
      (future-cancel @dev-watcher)
      nil)
    (println "Dev server is not running. Run '(start)' first.")))

(defn state []
  @system)
