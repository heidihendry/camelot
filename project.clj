(defproject camelot "0.1.0-SNAPSHOT"
  :description "Camera Trap Data Processing"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.drewnoakes/metadata-extractor "2.8.1"]
                 [prismatic/schema "1.0.5"]
                 [clj-time "0.11.0"]
                 [funcool/cats "1.2.1"]
                 [com.taoensso/tower "3.1.0-beta4"]
                 [commons-io/commons-io "2.4"]
                 [compojure "1.5.0"]
                 [ring/ring-defaults "0.1.5"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler camelot.core/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
