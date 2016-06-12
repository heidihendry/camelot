(ns camelot.report.module.builtin.columns.trap-station-session-camera-count
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :trap-station-session-camera-count
 {:calculate (partial col-util/calculate-count :trap-station-session-camera)
  :aggregate (partial col-util/aggregate-numeric :trap-station-session-camera-id)})
