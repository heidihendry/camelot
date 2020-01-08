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
  [state detector-state-ref cmd-mult result-ch archive-ch event-ch]
  (let [cmd-ch (async/tap cmd-mult (async/chan))
        retry-ch (async/chan (async/sliding-buffer 10000))
        ch (async/chan (async/sliding-buffer 10000))]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch retry-ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (log/info "Detector poll stopped")

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
            (when (:valid-at v)
              (if (< (:retries v) retry-limit)
                (let [delay (max 0 (- (tc/to-long (:valid-at v)) (tc/to-long (t/now))))]
                  (async/>! event-ch {:action :poll-task-retried
                                      :subject :task
                                      :subject-id task-id
                                      :meta {:dimension1 "retries" :metric1 (:retries v)}})
                  (log/info "Retrying poll with task id" task-id "in" (/ delay 1000.0) "seconds")
                  (async/<! (async/timeout delay))
                  (async/go (async/>! ch v)))
                (async/>! event-ch {:action :poll-task-retry-limit-reached
                                    :subject :task
                                    :subject-id task-id})))
            (recur))

          ch
          (let [task-id (:subject-id v)]
            (if (state/completed-task? @detector-state-ref task-id)
              (log/warn "Processing task already found to be completed. Skipping." task-id)
              (do
                (async/>! event-ch v)
                (log/info "Fetching results for task id" task-id)
                (try
                  (let [resp (client/get-task state task-id)]
                    (condp = (:status resp)
                      "COMPLETED"
                      (do
                        (async/>! event-ch {:action :poll-task-completed
                                            :subject :task
                                            :subject-id task-id
                                            :meta {:dimension1 "images"
                                                   :metric1 (count (-> resp :result :images))}})
                        (analytics/track-timing state {:hit-type "timing"
                                                       :category "detector"
                                                       :variable "poll-task-scheduled-to-completed"
                                                       :time (- (tc/to-long (t/now)) (tc/to-long (:created v)))
                                                       :ni true})
                        (log/info "Found results for" task-id)
                        (doseq [image (-> resp :result :images)]
                          (try
                            (if-let [file (:file image)]
                              (if-let [media-id (edn/read-string (second (re-matches #".*?([0-9]+)\.[a-zA-Z]+$" file)))]
                                (let [payload (build-payload (:result resp) image)]
                                  (log/info "Scheduling result processing for" media-id)
                                  (async/>! result-ch (event/to-image-detection-event media-id payload)))
                                (do
                                  (async/>! event-ch {:action :poll-media-id-missing
                                                      :subject :task
                                                      :subject-id task-id})
                                  (log/error "Could not find media ID in" file)))
                              (do
                                (async/>! event-ch {:action :poll-image-file-data-missing
                                                    :subject :task
                                                    :subject-id task-id})
                                (log/error "Could not find file in image detection data" image)))
                            (catch Exception e
                              (async/>! event-ch {:action :poll-suggestion-creation-exception
                                                  :subject :task
                                                  :subject-id task-id})
                              (log/error "Could not find media ID in" (:file image) e))))
                        (async/>! archive-ch (event/to-archive-task-event task-id))
                        (state/set-task-status! detector-state-ref task-id "completed"))

                      "FAILED"
                      (do
                        (async/>! event-ch {:action :poll-task-failed
                                            :subject :task
                                            :subject-id task-id})
                        (log/info "Failed to get results for" task-id)
                        (state/set-task-status! detector-state-ref task-id "failed"))

                      ;; TODO distinguish retryable and non-retryable failures
                      "PROBLEM"
                      nil

                      "SUBMITTED"
                      (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                          :retries (if-let [r (:retries v)] (inc r) 1))))

                      "RUNNING"
                      (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) (t/minutes 1))
                                                          :retries (if-let [r (:retries v)] (inc r) 1))))))
                  (catch Exception e
                    (log/warn "Error while fetching data for" task-id ". " e)))))
            (recur)))))
    ch))
