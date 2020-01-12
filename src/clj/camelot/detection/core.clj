(ns camelot.detection.core
  "Wildlife detection."
  (:require
   [camelot.detection.archive :as archive]
   [camelot.detection.client :as client]
   [camelot.detection.submit :as submit]
   [camelot.detection.poll :as poll]
   [camelot.detection.prepare :as prepare]
   [camelot.detection.bootstrap :as bootstrap]
   [camelot.detection.upload :as upload]
   [camelot.detection.result :as result]
   [camelot.detection.watchdog :as watchdog]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

;; TODO implement mechanism to force-scheduling of newly finalised TS sessions
;; TODO consider connection pooling
(defn run
  "Run the detector"
  [state detector-state-ref cmd-pub-ch cmd-mult]
  (let [event-ch (async/chan (async/sliding-buffer 1000))]
    (try
      (client/account-auth state)
      (let [archive-ch (archive/run state detector-state-ref cmd-mult event-ch)
            result-ch (result/run state detector-state-ref cmd-mult event-ch)
            poll-ch (poll/run state detector-state-ref cmd-mult result-ch archive-ch event-ch)
            submit-ch (submit/run state detector-state-ref cmd-mult poll-ch archive-ch event-ch)
            upload-ch (upload/run state detector-state-ref cmd-mult submit-ch event-ch)
            prepare-ch (prepare/run state detector-state-ref cmd-mult upload-ch poll-ch event-ch)]
        (bootstrap/run state detector-state-ref cmd-mult prepare-ch event-ch)
        (watchdog/run state detector-state-ref cmd-mult cmd-pub-ch event-ch))
      (async/go
        (async/>! event-ch {:action :running
                            :subject :system-status}))
      (catch Exception e
        (log/error "Authentication for detector failed:" e)
        (async/go
          (async/>! event-ch {:action :detector-authentication-failed
                              :subject :system-status}))))
    event-ch))
