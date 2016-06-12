(ns camelot.report-builder.module.builtin.column.trap-station-session-camera-count
  (:require [camelot.report-builder.module.core :as module]
            [camelot.report-builder.module.column-util :as col-util]))

(module/register-column
 :trap-station-session-camera-count
 {:calculate (partial col-util/calculate-count :trap-station-session-camera)
  :aggregate (partial col-util/aggregate-numeric :trap-station-session-camera-id)})
