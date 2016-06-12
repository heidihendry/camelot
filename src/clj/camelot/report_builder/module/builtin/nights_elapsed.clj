(ns camelot.report-builder.module.builtin.nights-elapsed
  (:require [camelot.report-builder.module.column-util :as col-util]
            [camelot.report-builder.module.core :as module]))

(module/add-column
 :nights-elapsed
 {:calculate col-util/calculate-nights-elapsed
  :aggregate col-util/aggregate-by-trap-station})
