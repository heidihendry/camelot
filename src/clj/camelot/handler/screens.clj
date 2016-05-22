(ns camelot.handler.screens
  (:require [camelot.model.screens :as model]
            [smithy.core :as smithy]
            [compojure.core :refer [ANY GET PUT POST DELETE context]]
            [camelot.processing.settings :refer [gen-state config cursorise decursorise]]
            [ring.util.response :as r]))

(defn translate-fn
  "Return a key translation function for the smithy build process."
  [state]
  (fn [resource lookup]
    ((:translate state) (keyword (format "%s/%s" (name resource)
                                         (subs (str lookup) 1))))))

(defn all-screens
  "Build the available screen smiths."
  [state]
  (smithy/build-smiths model/smiths (translate-fn state) state))

(def routes
  (context "/screens" []
           (GET "/" [] (r/response (all-screens (gen-state (config)))))))
