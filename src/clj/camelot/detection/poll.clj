(ns camelot.detection.poll
  (:require
   [camelot.services.analytics :as analytics]
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [clojure.edn :as edn]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

(def ^:private retry-limit 20)

(defn- build-payload
  [result image]
  (-> result
      (dissoc :images)
      (assoc :image image)))

(defn run
  "Create suggestions for values placed on the returned channel."
  [state detector-state-ref cmd-mult result-ch event-ch]
  (let [cmd-ch (async/chan)
        ch (async/chan (async/sliding-buffer 10000))]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector poll stopped")
            (recur))

          ch
          (let [task-id (:subject-id v)]
            (async/>! event-ch v)
            (when (:valid-at v)
              (if (< (:retries v) retry-limit)
                (let [delay (max 0 (- (tc/to-long (:valid-at v)) (tc/to-long (t/now))))]
                  (analytics/track state {:category "detector"
                                          :action "poll-task-retried"
                                          :label "task"
                                          :label-value task-id
                                          :dimension1 "retries"
                                          :metric1 (:retries v)
                                          :ni true})
                  (log/info "Retrying poll with task id" task-id "in" (/ delay 1000.0) "seconds")
                  (async/<! (async/timeout delay)))
                (analytics/track state {:category "detector"
                                        :action "poll-task-retry-limit-reached"
                                        :label "task"
                                        :label-value task-id
                                        :ni true})))
            (log/info "Fetching results for task id" task-id)
            (try
              (let [resp (client/get-task state task-id)]
                (condp = (:status resp)
                  "COMPLETED"
                  (do
                    (analytics/track state {:category "detector"
                                            :action "poll-task-completed"
                                            :label "task"
                                            :label-value task-id
                                            :dimension1 "images"
                                            :metric1 (count (-> resp :result :images))
                                            :ni true})
                    (analytics/track-timing state {:hit-type "timing"
                                                   :category "detector"
                                                   :variable "poll-task-scheduled-to-completed"
                                                   :time (- (tc/long (:created v)) (tc/long (t/now)))
                                                   :ni true})
                    (log/info "Found results for " task-id)
                    (doseq [image (-> resp :result :images)]
                      (try
                        (if-let [file (:file image)]
                          (if-let [media-id (edn/read-string (second (re-matches #".*?([0-9]+)\.[a-zA-Z]+$" file)))]
                            (let [payload (build-payload (:result resp) image)]
                              (log/info "Scheduling result processing for" media-id)
                              (async/>! result-ch (event/to-image-detection-event media-id payload)))
                            (log/error "Could not find media ID in" file))
                          (log/error "Could not find file in image detection data" image))
                        (catch Exception e
                          (log/error "Could not find media ID in" (:file image) e)))))

                  "FAILED"
                  (do
                    (analytics/track state {:category "detector"
                                            :action "poll-task-failed"
                                            :label "task"
                                            :label-value task-id
                                            :ni true})
                    (log/info "Failed to get results for" task-id)
                    (state/set-task-status! detector-state-ref task-id "failed"))

                  ;; TODO distinguish retryable and non-retryable failures
                  "PROBLEM"
                  nil

                  "SUBMITTED"
                  (async/go (async/>! ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                :retries (if-let [r (:retries v)] (inc r) 1))))

                  "RUNNING"
                  (async/go (async/>! ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                :retries (if-let [r (:retries v)] (inc r) 1))))))
              (catch Exception e
                (log/warn "Error while fetching data for" task-id ". " e)))
            (recur)))))
    ch))
