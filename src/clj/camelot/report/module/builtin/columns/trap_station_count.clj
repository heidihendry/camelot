(ns camelot.report.module.builtin.columns.trap-station-count
  (:require
   [camelot.report.module
    [column-util :as col-util]
    [core :as module]]))

(module/register-column
 :trap-station-count
 {:calculate (partial col-util/calculate-count :trap-station)
  :aggregate (partial col-util/aggregate-numeric :trap-station-id)})
