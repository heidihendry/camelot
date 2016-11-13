(ns camelot.report.module.builtin.columns.trap-station-session-count
  (:require
   [camelot.report.module
    [column-util :as col-util]
    [core :as module]]))

(module/register-column
 :trap-station-session-count
 {:calculate (partial col-util/calculate-count :trap-station-session)
  :aggregate (partial col-util/aggregate-numeric :trap-station-session-id)})
