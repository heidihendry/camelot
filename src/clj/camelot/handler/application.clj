(ns camelot.handler.application
  (:require [camelot.model.application :as app-model]
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

(def metadata-paths (flatten-metadata-structure app-model/metadata-structure))

(defn get-metadata
  "Return paths alongside a (translated) description of the metadata represented
  by that path."
  [state]
  (into {} (map #(hash-map % (putil/path-description state %)) metadata-paths)))

(defn get-version
  "Get the version string from the system properties or the jar metadata."
  []
  (or (System/getProperty "camelot.version")
      (app/version-property-from-pom 'camelot)))

(defn get-nav-menu
  "Return the application navigation menu."
  [state]
  (app-model/nav-menu state))

(def routes
  (context "/application" []
           (GET "/metadata" [] (r/response (get-metadata (app/gen-state (conf/config)))))
           (GET "/" [] (r/response {:version (get-version)
                                    :nav (get-nav-menu (app/gen-state (conf/config)))}))))
