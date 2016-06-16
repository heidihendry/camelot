(ns camelot.report.module.builtin.columns.site-count
  (:require [camelot.report.module
             [column-util :as col-util]
             [core :as module]]))

(module/register-column
 :site-count
 {:calculate (partial col-util/calculate-count :site)
  :aggregate (partial col-util/aggregate-numeric :site-id)})
