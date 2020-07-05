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

(defonce ^:private bootstrap-ch (atom nil))

;; TODO implement mechanism to force-scheduling of newly finalised TS sessions
;; TODO consider connection pooling
(defn run
  "Run the detector"
  [system-state detector-state-ref cmd-pub-ch cmd-mult learn-ch]
  (let [event-ch (async/chan (async/sliding-buffer 1000))]
    (try
      (client/account-auth system-state)
      (swap! bootstrap-ch #(or % (bootstrap/run system-state detector-state-ref cmd-mult event-ch)))
      (let [archive-chans (archive/run system-state detector-state-ref event-ch)
            result-chans (result/run system-state detector-state-ref event-ch learn-ch)
            poll-chans (poll/run system-state detector-state-ref result-chans archive-chans event-ch)
            submit-chans (submit/run system-state detector-state-ref poll-chans archive-chans event-ch)
            upload-chans (upload/run system-state detector-state-ref submit-chans event-ch)]
        (prepare/run system-state detector-state-ref @bootstrap-ch cmd-mult upload-chans poll-chans event-ch)
        (watchdog/run system-state detector-state-ref cmd-mult cmd-pub-ch event-ch))
      (async/go
        (async/>! event-ch {:action :running
                            :subject :system-status}))
      (catch Exception e
        (log/error "Authentication for detector failed:" e)
        (async/go
          (async/>! event-ch {:action :detector-authentication-failed
                              :subject :system-status}))))
    event-ch))
