(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.core-test]))

(enable-console-print!)

(doo-tests 'camelot.core-test)
