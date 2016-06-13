(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.core-test]
   [camelot.component.library-test]
   [om-datepicker.test-components]))

(enable-console-print!)


(doo-tests 'om-datepicker.test-components
           'camelot.core-test
           'camelot.component.library-test)
