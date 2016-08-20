(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.nav-test]
   [camelot.component.albums-test]
   [camelot.util.filter-test]
   [om-datepicker.test-components]
   [typeahead.core]
   [camelot.util.trap-station-test]))

(enable-console-print!)

(doo-tests 'om-datepicker.test-components
           'typeahead.core
           'camelot.nav-test
           'camelot.component.albums-test
           'camelot.util.filter-test
           'camelot.util.trap-station-test)
