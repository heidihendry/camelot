(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.core-test]
   [camelot.util.filter-test]
   [om-datepicker.test-components]
   [camelot.util.trap-station-test]))

(enable-console-print!)

(doo-tests 'om-datepicker.test-components
           'camelot.core-test
           'camelot.util.filter-test
           'camelot.util.trap-station-test)
