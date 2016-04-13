(defproject camelot "0.2.3-SNAPSHOT"
  :description "Camelot - Camera Trap Management"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[bk/ring-gzip "0.1.1"]
                 [clj-time "0.11.0"]
                 [cljs-http "0.1.39"]
                 [cljsjs/react-with-addons "0.14.3-0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [com.drewnoakes/metadata-extractor "2.8.1"]
                 [com.taoensso/tower "3.1.0-beta4"]
                 [commons-io/commons-io "2.4"]
                 [compojure "1.5.0"]
                 [environ "1.0.2"]
                 [incanter/incanter-core "1.5.7"]
                 [midje "1.8.3"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha32" :exclusions [com.cognitect/transit-cljs cljsjs/react]]
                 [prismatic/schema "1.0.5"]
                 [ring "1.4.0"]
                 [ring-transit "0.1.4"]
                 [ring.middleware.logger "0.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-environ "1.0.1"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "dev"]

  :test-paths ["test/clj"]

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  :uberjar-name "camelot.jar"

  :main camelot.core
  
  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (run) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]

                :figwheel true
                ;; Alternatively, you can configure a function to run every time figwheel reloads.
                ;; :figwheel {:on-jsload "camelot.core/on-figwheel-reload"}

                :compiler {:main camelot.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/camelot.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}}}

  ;; When running figwheel from nREPL, figwheel will read this configuration
  ;; stanza, but it will read it without passing through leiningen's profile
  ;; merging. So don't put a :figwheel section under the :dev profile, it will
  ;; not be picked up, instead configure figwheel here on the top level.

  :figwheel {;; :http-server-root "public"       ;; serve static assets from resources/public/
             ;; :server-port 3449                ;; default
             ;; :server-ip "127.0.0.1"           ;; default
             :css-dirs ["resources/public/css"]  ;; watch and update CSS

             ;; Instead of booting a separate server on its own port, we embed
             ;; the server ring handler inside figwheel's http-kit server, so
             ;; assets and API endpoints can all be accessed on the same host
             ;; and port. If you prefer a separate server process then take this
             ;; out and start the server with `lein run`.
             :ring-handler user/http-handler

             ;; Start an nREPL server into the running figwheel process. We
             ;; don't do this, instead we do the opposite, running figwheel from
             ;; an nREPL process, see
             ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
             ;; :nrepl-port 7888

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             :server-logfile "log/figwheel.log"}

  :doo {:build "test"
        :alias {:browsers [:chrome :phantom]}}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.1"]
                             [figwheel-sidecar "0.5.1"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]]

              :plugins [[lein-figwheel "0.5.1" :exclusions [org.clojure/clojure]]
                        [lein-doo "0.1.6" :exclusions [org.clojure/clojure]]]

              :cljsbuild {:builds
                          {:test
                           {:source-paths ["src/cljs" "test/cljs"]
                            :compiler
                            {:output-to "resources/public/js/compiled/testable.js"
                             :main camelot.test-runner
                             :optimizations :none}}}}}

             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :hooks [leiningen.cljsbuild]
              :omit-source true
              :global-vars {*warn-on-reflection* true}
              :aot :all
              :cljsbuild {:builds
                          {:app
                           {:source-paths ^:replace ["src/cljs"]
                            :compiler
                            {:optimizations :advanced
                             :pretty-print false}}}}}})
