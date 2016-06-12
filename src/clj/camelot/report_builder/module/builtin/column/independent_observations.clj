(ns camelot.report-builder.module.builtin.column.independent-observations
  (:require [camelot.report-builder.module.core :as module]
            [camelot.report-builder.module.column-util :as col-util]
            [camelot.import.album :as album]))

(module/register-column
 :independent-observations
 {:calculate col-util/calculate-independent-observations
  :aggregate col-util/aggregate-by-trap-station})
