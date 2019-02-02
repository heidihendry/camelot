(ns camelot.test-runner
  (:require
   [cljs.test :refer-macros [run-tests] :refer [report]]
    [figwheel.main.async-result :as async-result]
    [typeahead.core-test]
    [camelot.nav-test]
    [camelot.util.trap-station-test]))

;; tests can be asynchronous, we must hook test end
(defmethod report [:cljs.test/default :end-run-tests] [test-data]
  (if (cljs.test/successful? test-data)
    (async-result/send "Tests passed!")
    (async-result/throw-ex (ex-info "Tests Failed" test-data))))

(defn -main [& args]
  (println "Running Camelot tests!")
  (run-tests 'typeahead.core-test 'camelot.nav-test 'camelot.util.trap-station-test)
  ;; return a message to the figwheel process that tells it to wait
  [:figwheel.main.async-result/wait 10000])
