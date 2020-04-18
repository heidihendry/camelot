(ns camelot.detection.archive
  (:require
   [camelot.detection.datasets :as datasets]
   [camelot.detection.client :as client]
   [camelot.detection.state :as state]
   [camelot.detection.util :as util]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defn run
  "Archive tasks."
  [system-state detector-state-ref event-ch]
  (let [cmd-ch (async/chan (async/dropping-buffer 100))
        ch (async/chan)]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (log/info "Detector archive stopped")

            :pause
            (util/pause cmd-ch identity)

            (recur))

          ch
          (datasets/with-context {:system-state system-state
                                  :ctx v}
            [state]
            (async/>! event-ch v)
            (let [task-id (:subject-id v)]
              (try
                (client/archive-task state task-id)
                (state/archive-task! (datasets/detector-state state detector-state-ref) task-id)
                (log/info "Archival successful for task" task-id)
                (async/>! event-ch {:action :archive-success
                                    :subject :task
                                    :subject-id task-id})
                (catch Exception e
                  (log/warn "Archival failed for task" task-id e)
                  (async/>! event-ch {:action :archive-failed
                                      :subject :task
                                      :subject-id task-id}))))
            (recur)))))
    [ch cmd-ch]))
