(ns camelot.report.module.builtin.columns.nights-elapsed
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :specific-nights-elapsed
 {:calculate col-util/calculate-specific-nights-elapsed
  :aggregate col-util/aggregate-by-trap-station})

(module/register-column
 :nights-elapsed
 {:calculate col-util/calculate-nights-elapsed
  :aggregate col-util/aggregate-by-trap-station})
