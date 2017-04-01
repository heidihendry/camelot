(defproject camelot "1.2.3-SNAPSHOT"
  :description "Camera trap software for data management and analysis. Designed for researchers and conservationists."
  :url "http://gitlab.com/camelot-project/camelot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/tools.namespace "0.2.11"]

                 [org.apache.derby/derby "10.12.1.1"]
                 [org.omcljs/om "1.0.0-alpha32"
                  :exclusions [com.cognitect/transit-cljs cljsjs/react]]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.0"]
                 [ragtime "0.5.3"]
                 [resauce "0.1.0"]
                 [yesql "0.5.2"]
                 [ring.middleware.logger "0.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring "1.4.0"]
                 [ring-transit "0.1.4"]
                 [secretary "1.2.3"]
                 [com.taoensso/tower "3.1.0-beta4"]

                 [bk/ring-gzip "0.1.1"]
                 [cheshire "5.6.1"]
                 [clj-http "2.2.0"]
                 [clj-time "0.11.0"]
                 [cljs-http "0.1.39"]
                 [cljsjs/react-with-addons "0.14.3-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.drewnoakes/metadata-extractor "2.9.1"]
                 [com.luckycatlabs/SunriseSunsetCalculator "1.2"]
                 [commons-io/commons-io "2.4"]
                 [environ "1.0.2"]
                 [net.mikera/imagez "0.10.0"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [prismatic/schema "1.0.5"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-environ "1.0.2"]
            [com.jakemccrary/lein-test-refresh "0.18.1"]]

  :min-lein-version "2.6.1"
  :env {:squiggly "{:checkers [:eastwood :kibit]}"}
  :source-paths ["src/cljc" "src/clj" "src/cljs" "dev"]
  :test-paths ["test/cljc" "test/clj"]
  :test-refresh {:refresh-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :watch-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :changes-only true
                 :quiet true}
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :uberjar-name "camelot.jar"
  :main camelot.core
  :repl-options {:init-ns user}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljc" "src/cljs"]
                :figwheel true
                :compiler {:main camelot.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/camelot.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}}}
  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler user/http-handler
             :server-logfile "log/figwheel.log"}

  :doo {:build "test"
        :alias {:browsers [:chrome :phantom]}}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.4"]
                             [figwheel-sidecar "0.5.4"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]
                             [org.apache.derby/derbytools "10.12.1.1"]]
              :env {:camelot-dev-mode "true"}
              :plugins [[lein-figwheel "0.5.8" :exclusions [org.clojure/clojure]]
                        [lein-doo "0.1.7" :exclusions [org.clojure/clojure]]]
              :cljsbuild {:builds
                          {:test
                           {:source-paths ["src/cljc" "src/cljs" "test/cljc" "test/cljs"]
                            :compiler
                            {:output-to "resources/public/js/compiled/testable.js"
                             :main camelot.test-runner
                             :optimizations :none}}}}}

             :uberjar
             {:source-paths ^:replace ["src/clj" "src/cljc"]
              :hooks [leiningen.cljsbuild]
              :omit-source true
              :global-vars {*warn-on-reflection* true}
              :aot :all
              :cljsbuild {:builds
                          {:app
                           {:source-paths ^:replace ["src/cljc" "src/cljs"]
                            :compiler
                            {:optimizations :advanced
                             :externs ["resources/public/lib/ga.js"]
                             :closure-warnings {:externs-validation :off}
                             :pretty-print false}}}}}})
