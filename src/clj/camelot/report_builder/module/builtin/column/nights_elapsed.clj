(ns camelot.report-builder.module.builtin.column.nights-elapsed
  (:require [camelot.report-builder.module.column-util :as col-util]
            [camelot.report-builder.module.core :as module]))

(module/register-column
 :nights-elapsed
 {:calculate col-util/calculate-nights-elapsed
  :aggregate col-util/aggregate-by-trap-station})
