(ns camelot.handler.settings
  (:require [camelot.model.settings :as ms]
            [camelot.processing.util :as putil]
            [camelot.util
             [config :as conf]
             [application :as app]
             [rest :as rest-util]]
            [compojure.core :refer [ANY context DELETE GET POST PUT]]
            [ring.util.response :as r]))

(defn- flatten-metadata-structure
  "Transfor metadata structure in to a vector of paths"
  [md]
  (vec (reduce #(into %1 (if (= (type %2) clojure.lang.Keyword)
                              [[%2]]
                              (mapv (fn [v] [(first %2) v]) (second %2))))
                   []
                   md)))

(def metadata-paths (flatten-metadata-structure ms/metadata-structure))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))

(defn settings-save
  "Save a configuration."
  [config]
  (conf/save-config config))

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (app/version-property-from-pom 'camelot)))

(defn get-nav-menu
  [state]
  (ms/nav-menu state))

(def routes
  (context "/settings" []
           (GET "/" [] (r/response (rest-util/cursorise (conf/config))))
           (PUT "/" [data] (r/response (settings-save (rest-util/decursorise data))))
           (GET "/metadata" [] (r/response (get-metadata (app/gen-state (conf/config)))))
           (GET "/application" [] (r/response {:version (get-version)
                                               :nav (get-nav-menu (app/gen-state (conf/config)))}))))
