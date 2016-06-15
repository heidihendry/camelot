(ns camelot.report.module.builtin.columns.nights-elapsed
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :nights-elapsed
 {:calculate col-util/calculate-nights-elapsed
  :aggregate col-util/aggregate-by-trap-station})
