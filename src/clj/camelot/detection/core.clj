(ns camelot.detection.core
  "Wildlife detection."
  (:require
   [camelot.detection.submit :as submit]
   [camelot.detection.poll :as poll]
   [camelot.detection.prepare :as prepare]
   [camelot.detection.bootstrap :as bootstrap]
   [camelot.detection.upload :as upload]
   [camelot.detection.result :as result]
   [clojure.core.async :as async]))

;; TODO check container expiry and recreat task if necessary
;; TODO implement mechanism to force-scheduling of newly finalised TS sessions
(defn run
  "Run the detector"
  [state detector-state-ref cmd-mult]
  (let [event-ch (async/chan (async/sliding-buffer 1000))
        result-ch (result/run state detector-state-ref cmd-mult event-ch)
        poll-ch (poll/run state detector-state-ref cmd-mult result-ch event-ch)
        submit-ch (submit/run state detector-state-ref cmd-mult poll-ch event-ch)
        upload-ch (upload/run state detector-state-ref cmd-mult submit-ch event-ch)
        prepare-ch (prepare/run state detector-state-ref cmd-mult upload-ch poll-ch event-ch)]
    (bootstrap/run state detector-state-ref cmd-mult prepare-ch)
    event-ch))
