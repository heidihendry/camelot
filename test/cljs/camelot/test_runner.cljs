(ns camelot.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [camelot.nav-test]
   [typeahead.core-test]
   [camelot.util.trap-station-test]))

(doo-tests 'typeahead.core-test
           'camelot.nav-test
           'camelot.util.trap-station-test)
