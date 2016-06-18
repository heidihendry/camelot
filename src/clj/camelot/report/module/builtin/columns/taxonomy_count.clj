(ns camelot.report.module.builtin.columns.taxonomy-count
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :taxonomy-count
 {:calculate (partial col-util/calculate-count :taxonomy)
  :aggregate col-util/aggregate-by-species})
