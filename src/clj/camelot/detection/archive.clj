(ns camelot.detection.result
  (:require
   [camelot.detection.client :as client]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defn run
  "Archive tasks."
  [state detector-state-ref cmd-mult event-ch]
  (let [cmd-ch (async/chan)
        ch (async/chan)]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector archive stopped")
            (recur))

          ch
          (do
            (async/>! event-ch v)
            (let [task-id (:subject-id v)]
              (try
                (client/archive-task state task-id)
                (log/info "Archival successful for task" task-id)
                (async/>! event-ch {:action :archive-success
                                    :subject :task
                                    :subject-id task-id})
                (catch Exception e
                  (log/warn "Archival failed for task" task-id)
                  (async/>! event-ch {:action :archive-failed
                                      :subject :task
                                      :subject-id task-id}))))
            (recur)))))
    ch))

