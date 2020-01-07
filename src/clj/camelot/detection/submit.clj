(ns camelot.detection.submit
  (:require
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

(defn- pending?
  [detector-state task-id]
  (let [pending (->> task-id
                     (state/media-for-task detector-state)
                     (filter (partial state/upload-pending? detector-state)))]
    (boolean (seq pending))))

(defn- some-completed?
  [detector-state task-id]
  (->> task-id
       (state/media-for-task detector-state)
       (some (partial state/upload-completed? detector-state))))

(def ^:private retry-limit 3)

(defn run
  "Submit tasks for processing."
  [state detector-state-ref cmd-mult poll-ch archive-ch event-ch]
  (let [cmd-ch (async/tap cmd-mult (async/chan))
        retry-ch (async/chan (async/sliding-buffer 10000))
        ch (async/chan (async/sliding-buffer 10000))
        int-ch (async/chan)]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch retry-ch ch int-ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (log/info "Detector submit stopped")

            :pause
            (do
              (loop []
                (let [{:keys [cmd]} (async/<! cmd-ch)]
                  (when-not (= cmd :resume)
                    (recur))))
              (recur))

            (recur))

          retry-ch
          (let [task-id (:subject-id v)]
            (let [delay (max 0 (- (tc/to-long (:valid-at v)) (tc/to-long (t/now))))]
              (log/info "Retrying check with task id" task-id
                        "in" (/ delay 1000.0) "seconds")
              (async/<! (async/timeout delay)))
            (if (< (:retries v) retry-limit)
              (if (pending? @detector-state-ref task-id)
                (do
                  (async/>! event-ch {:action :submit-retry
                                      :subject :task
                                      :subject-id task-id})
                  (async/go (async/>! retry-ch (-> v
                                                   (assoc :valid-at (t/plus (t/now) (t/minutes 1)))
                                                   (update :retries inc)))))
                (if (some-completed? @detector-state-ref task-id)
                  (async/go (async/>! int-ch v))
                  (do
                    (async/>! event-ch {:action :submit-no-completed-uploads
                                        :subject :task
                                        :subject-id task-id})
                    (log/warn "No uploads completed for" task-id))))
              (do
                (log/warn "Retry limit reached for" task-id)
                (async/>! event-ch {:action :submit-retry-limit-reached
                                    :subject :task
                                    :subject-id task-id})))
            (recur))

          ch
          (let [task-id (:subject-id v)]
            (log/info "Presubmit check with task id" (:subject-id v))
            (if (pending? @detector-state-ref task-id)
              (do
                (log/info "Scheduling retry")
                (async/>! event-ch {:action :submit-retry
                                    :subject :task
                                    :subject-id task-id})
                (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                     :retries 1)))
                (log/info "Scheduled retry"))
              (if (some-completed? @detector-state-ref task-id)
                (do
                  (async/go (async/>! int-ch (event/to-submit-event task-id)))
                  (log/info "Placed on internal channel" task-id))
                (do
                  (async/>! event-ch {:action :submit-no-completed-uploads
                                      :subject :task
                                      :subject-id task-id})
                  (log/warn "No uploads completed for" task-id)
                  (async/>! archive-ch (event/to-archive-task-event task-id)))))
            (recur))

          int-ch
          (do
            (async/>! event-ch v)
            (let [task-id (:subject-id v)]
              (try
                (log/info "Submit task with id" task-id)
                (client/submit-task state task-id)
                (state/set-task-status! detector-state-ref task-id "submitted")
                (async/>! event-ch {:action :submit-task-call-success
                                    :subject :task
                                    :subject-id task-id})
                (async/>! poll-ch (event/to-check-result-event task-id))
                (catch Exception e
                  (log/info "Submit call for task" task-id "failed with exception" e)
                  (async/>! event-ch {:action :submit-task-call-failed
                                      :subject :task
                                      :subject-id task-id}))))
            (recur)))))
    ch))
