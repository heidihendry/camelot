(ns camelot.report.module.builtin.columns.independent-observations
  (:require
   [camelot.report.module
    [column-util :as col-util]
    [core :as module]]))

(module/register-column
 :independent-observations
 {:calculate col-util/calculate-independent-observations
  :aggregate col-util/aggregate-by-trap-station-session})
