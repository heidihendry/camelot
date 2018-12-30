(defproject camelot "1.4.6-SNAPSHOT"
  :description "Manage and analyse camera trap data. Designed for researchers and conservationists."
  :url "http://gitlab.com/camelot-project/camelot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/test.check "0.9.0" :scope "test"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.cli "0.3.5"]

                 [com.cognitect/transit-clj "0.8.313"]
                 [org.apache.derby/derby "10.12.1.1"]
                 [org.omcljs/om "1.0.0-beta1"
                  :exclusions [com.cognitect/transit-clj
                               com.cognitect/transit-cljs]]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.6.1"]
                 [ragtime "0.5.3"]
                 [resauce "0.1.0"]
                 [yesql "0.5.2"]
                 [honeysql "0.9.2"]
                 [riddley "0.1.4"]
                 [onelog "0.5.0"]
                 [ring.middleware.logger "0.5.0" :exclusions [onelog]]
                 [ring/ring-defaults "0.3.2"]
                 [ring "1.6.3"]
                 [ring-transit "0.1.4" :exclusions [com.cognitect/transit-clj]]
                 ;; Required by tower; later version avoids shadowing of clojure.core
                 [com.taoensso/encore "2.97.0"]
                 [com.taoensso/tower "3.1.0-beta4" :exclusions [com.taoensso/encore]]
                 [commons-io/commons-io "2.4"]
                 [environ "1.1.0"]
                 [prismatic/schema "1.1.6"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [com.drewnoakes/metadata-extractor "2.11.0"]
                 [medley "1.0.0"]
                 [clj-commons/secretary "1.2.4" :exclusions [org.clojure/clojurescript]]
                 [bk/ring-gzip "0.1.1"]
                 [cheshire "5.8.0"]
                 [clj-http "2.2.0"]
                 [clj-time "0.14.2"]
                 [cljs-http "0.1.45" :exclusions [com.cognitect/transit-clj]]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [com.luckycatlabs/SunriseSunsetCalculator "1.2"]
                 [net.mikera/imagez "0.10.0"]
                 [com.cemerick/url "0.1.1"]
                 [cider/piggieback "0.3.10" :scope "test" :exclusions [org.clojure/clojurescript]]
                 [weasel "0.7.0" :scope "test" :exclusions [org.clojure/clojurescript]]
                 [reloaded.repl "0.2.3" :scope "test"]
                 [org.apache.derby/derbytools "10.12.1.1" :scope "test"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]]

  :min-lein-version "2.6.1"
  :env {:squiggly "{:checkers [:kibit :eastwood] :eastwood-options {:exclude-linters [:unlimited-use] :add-linters [:unused-private-vars]}}"}

  :source-paths ["src/cljc" "src/clj" "src/cljs" "modules/market/src"]
  :test-paths ["test/cljc" "test/clj"]
  :test-refresh {:refresh-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :watch-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :changes-only true
                 :quiet true}

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/www/js"]
  :uberjar-name "camelot.jar"
  :main camelot.core
  :repl-options {:init-ns user}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.17"]
                             [figwheel-sidecar "0.5.17"]
                             [cider/piggieback "0.3.10"]
                             [org.clojure/core.logic "0.8.11"]
                             [org.clojure/tools.nrepl "0.2.13"]
                             [org.apache.derby/derbytools "10.12.1.1"]]
              :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
              :env {:camelot-dev-mode "true"}
              :source-paths ["src/cljc" "src/clj" "src/cljs" "dev" "modules/market/src"]
              :plugins [[lein-figwheel "0.5.17" :exclusions [org.clojure/clojure]]
                        [jonase/eastwood "0.3.3"]
                        [com.jakemccrary/lein-test-refresh "0.23.0"]]
              :figwheel {:css-dirs ["resources/www/css"]
                         :ring-handler user/http-handler
                         :server-logfile "log/figwheel.log"}
              :cljsbuild {:builds
                          [{:id "camelot"
                            :source-paths ["src/cljc" "src/cljs"]
                            :figwheel true
                            :compiler {:main camelot.core
                                       :asset-path "js/compiled/camelot"
                                       :output-to "resources/www/js/compiled/camelot.js"
                                       :output-dir "resources/www/js/compiled/camelot"
                                       :externs ["resources/www/js/ga.js"]
                                       :source-map-timestamp true}}]}}

             :test
             {:dependencies [[ring/ring-jetty-adapter "1.6.3"]
                             [com.bhauman/figwheel-main "0.2.0-SNAPSHOT"]]
              :source-paths ["src/clj" "src/cljc" "src/cljs" "test/clj" "test/cljc" "test/cljs" "modules/market/src"]
              :resource-paths ["target"]
              :cljsbuild {:builds
                          [{:id "test"
                            :source-paths ["src/cljc" "src/cljs" "test/cljc" "test/cljs"]
                            :compiler
                            {:output-to "resources/www/js/compiled/testable.js"
                             :main camelot.test-runner
                             :optimizations :none}}]}}

             :uberjar
             {:source-paths ^:replace ["src/clj" "src/cljc" "modules/market/src"]
              :hooks [leiningen.cljsbuild]
              :omit-source true
              :global-vars {*warn-on-reflection* true}
              :aot :all
              :cljsbuild {:builds
                          [{:id "camelot"
                            :source-paths ^:replace ["src/cljc" "src/cljs"]
                            :compiler
                            {:main camelot.core
                             :asset-path "js/compiled/camelot"
                             :output-to "resources/www/js/compiled/camelot.js"
                             :output-dir "resources/www/js/compiled/camelot"
                             :optimizations :advanced
                             :infer-externs true
                             :closure-warnings {:externs-validation :off}
                             :pretty-print false}}]}}})
