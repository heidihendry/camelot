(ns camelot.report-builder.module.builtin.trap-station-count
  (:require [camelot.report-builder.module.core :as module]
            [camelot.report-builder.module.column-util :as col-util]))


(module/add-column
 :trap-station-count
 {:calculate (partial col-util/calculate-count :trap-station)
  :aggregate (partial col-util/aggregate-numeric :trap-station-id)})
