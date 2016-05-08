(ns camelot.processing.screens
  (:require [camelot.model.screens :as model]
            [camelot.smithy.core :as smithy]))

(defn translate-fn
  "Return a key translation function for the smithy build process."
  [state]
  (fn [resource lookup]
    ((:translate state) (keyword (format "%s/%s" (name resource)
                                         (subs (str lookup) 1))))))

(defn smith
  "Build the available screen smiths."
  [state]
  (smithy/build-smiths model/smiths (translate-fn state) state))
