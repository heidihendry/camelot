(ns camelot.report-builder.module.builtin.column.media-count
  (:require [camelot.report-builder.module.core :as module]
            [camelot.report-builder.module.column-util :as col-util]))

(module/register-column
 :media-count
 {:calculate (partial col-util/calculate-count :media)
  :aggregate (partial col-util/aggregate-numeric :media-id)})
