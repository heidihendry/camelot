(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.nav-test]
   [om-datepicker.test-components]
   [typeahead.core-test]
   [camelot.util.trap-station-test]))

(enable-console-print!)

(doo-tests 'om-datepicker.test-components
           'typeahead.core-test
           'camelot.nav-test
           'camelot.util.trap-station-test)
