(ns camelot.report.module.builtin.columns.taxonomy-extras
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(defn calculate-species-name
  [state data]
  (map
   #(assoc % :species-name (if (and (:taxonomy-genus %) (:taxonomy-species %))
                             (format "%s %s" (:taxonomy-genus %) (:taxonomy-species %))
                             nil))
   data))

(module/register-column
 :species-name
 {:calculate calculate-species-name})

(module/register-column
 :taxonomy-count
 {:calculate (partial col-util/calculate-count :taxonomy)
  :aggregate col-util/aggregate-by-species})
