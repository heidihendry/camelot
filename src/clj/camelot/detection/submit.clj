(ns camelot.detection.submit
  (:require
   [camelot.services.analytics :as analytics]
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

(def retry-limit 3)

(defn run
  "Submit tasks for processing."
  [state detector-state-ref cmd-mult poll-ch event-ch]
  (let [cmd-ch (async/chan)
        retry-ch (async/chan (async/sliding-buffer 10000))
        ch (async/chan (async/sliding-buffer 10000))
        int-ch (async/chan)]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch retry-ch ch int-ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector submit stopped")
            (recur))

          retry-ch
          (let [task-id (:subject-id v)]
            (let [delay (max 0 (- (tc/to-long (:valid-at v)) (tc/to-long (t/now))))]
              (log/info "Retrying check with task id" task-id
                        "in" (/ delay 1000.0) "seconds")
              (async/<! (async/timeout delay)))
            (when (< (:retries v) retry-limit)
              (if (pending? @detector-state-ref task-id)
                (do
                  (analytics/track state {:category "detector"
                                          :action "submit-retry"
                                          :label "task"
                                          :label-value task-id
                                          :ni true})
                  (async/go (async/>! retry-ch (-> v
                                                   (assoc :valid-at (t/plus (t/now) (t/minutes 1)))
                                                   (update :retries inc)))))
                (if (some-completed? @detector-state-ref task-id)
                  (async/go (async/>! int-ch v))
                  (do
                    (analytics/track state {:category "detector"
                                            :action "submit-no-completed-uploads"
                                            :label "task"
                                            :label-value task-id
                                            :ni true})
                    (log/warn "No uploads completed for" task-id)))))
            (recur))

          ch
          (let [task-id (:subject-id v)]
            (log/info "Presubmit check with task id" (:subject-id v))
            (if (pending? @detector-state-ref task-id)
              (do
                (log/info "Scheduling retry")
                (analytics/track state {:category "detector"
                                        :action "submit-retry"
                                        :label "task"
                                        :label-value task-id
                                        :ni true})
                (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                     :retries 1)))
                (log/info "Scheduled retry"))
              (if (some-completed? @detector-state-ref task-id)
                (do
                  (log/info "Placing on internal channel" task-id)
                  (async/go (async/>! int-ch (event/to-submit-event task-id)))
                  (log/info "Placed on internal channel" task-id))
                (do
                  (analytics/track state {:category "detector"
                                          :action "submit-no-completed-uploads"
                                          :label "task"
                                          :label-value task-id
                                          :ni true})
                  (log/warn "No uploads completed for" task-id))))
            (recur))

          int-ch
          (do
            (async/>! event-ch v)
            (let [task-id (:subject-id v)]
              (try
                (log/info "Submit task with id" task-id)
                (analytics/track state {:category "detector"
                                        :action "submit-task-called"
                                        :label "task"
                                        :label-value task-id
                                        :ni true})
                (client/submit-task state task-id)
                (state/set-task-status! detector-state-ref task-id "submitted")
                (analytics/track state {:category "detector"
                                        :action "submit-task-call-success"
                                        :label "task"
                                        :label-value task-id
                                        :ni true})
                (async/>! poll-ch (event/to-check-result-event task-id))
                (catch Exception e
                  (log/info "Submit call for task" task-id "failed with exception" e)
                  (analytics/track state {:category "detector"
                                          :action "submit-task-call-failed"
                                          :label "task"
                                          :label-value task-id
                                          :ni true}))))
            (recur)))))
    ch))
