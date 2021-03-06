(defproject camelot "1.7.0-SNAPSHOT"
  :description "Manage and analyse camera trap data. Designed for researchers and conservationists."
  :url "http://gitlab.com/camelot-project/camelot"
  :license {:name "AGPL v3"
            :url "https://www.gnu.org/licenses/agpl-3.0.en.html"}

  :plugins [[lein-tools-deps "0.4.5"]
            [lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]
            [lein-nvd "1.4.0"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :min-lein-version "2.6.1"
  :env {:squiggly "{:checkers [:kibit :eastwood] :eastwood-options {:exclude-linters [:unlimited-use] :add-linters [:unused-private-vars]}}"}
  :source-paths ["src/cljc" "src/clj" "src/cljs"]
  :test-paths ["test/cljc" "test/clj"]
  :resource-paths ["resources"]

  :test-refresh {:refresh-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :watch-dirs ["test/clj" "test/cljc" "src/clj" "src/cljc"]
                 :changes-only true
                 :quiet true}

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/www/js/compiled"]
  :uberjar-name "camelot.jar"
  :main camelot.core
  :repl-options {:init-ns user}
  :profiles {:dev
             {:dependencies [[figwheel "0.5.18"]
                             [figwheel-sidecar "0.5.18"]
                             [org.apache.derby/derbytools "10.12.1.1"]
                             [cider/piggieback "0.4.1"]
                             [reloaded.repl "0.2.4"]]
              :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
              :env {:camelot-dev-mode "true"}
              :source-paths ["src/cljc" "src/clj" "src/cljs" "dev"]
              :plugins [[lein-figwheel "0.5.18" :exclusions [org.clojure/clojure]]
                        [jonase/eastwood "0.3.3"]
                        [com.jakemccrary/lein-test-refresh "0.23.0"]]
              :figwheel {:css-dirs ["resources/www/css"]
                         :ring-handler dev/http-handler
                         :server-port 5341
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
             {:dependencies [[ring/ring-jetty-adapter "1.6.3"]]
              :source-paths ["src/clj" "src/cljc" "src/cljs" "test/clj" "test/cljc" "test/cljs"]
              :resource-paths ["target"]
              :cljsbuild {:builds
                          [{:id "test"
                            :source-paths ["src/cljc" "src/cljs" "test/cljc" "test/cljs"]
                            :compiler
                            {:output-to "resources/www/js/compiled/testable.js"
                             :main camelot.test-runner
                             :optimizations :none}}]}
              :nvd {:fail-threshold 7
                    :verbose-summary true}}
             :uberjar
             {:source-paths ^:replace ["src/clj" "src/cljc"]
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
