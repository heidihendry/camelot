(ns camelot.report.module.builtin.columns.media-count
  (:require
   [camelot.report.module
    [column-util :as col-util]
    [core :as module]]))

(module/register-column
 :media-count
 {:calculate (partial col-util/calculate-count :media)
  :aggregate (partial col-util/aggregate-numeric :media-id)})
