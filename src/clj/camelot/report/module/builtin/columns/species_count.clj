(ns camelot.report.module.builtin.columns.species-count
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :species-count
 {:calculate (partial col-util/calculate-count :species)
  :aggregate col-util/aggregate-by-species})
