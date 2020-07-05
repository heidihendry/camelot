(ns camelot.detection.poll
  (:require
   [camelot.detection.datasets :as datasets]
   [camelot.services.analytics :as analytics]
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [camelot.detection.util :as util]
   [clojure.edn :as edn]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

(def ^:private retry-limit 10)
(def ^:private retry-timeout (t/minutes 5))

(defn- build-payload
  [result image]
  (-> result
      (dissoc :images)
      (assoc :image image)))

(defn run
  "Create suggestions for values placed on the returned channel."
  [system-state detector-state-ref [result-ch result-cmd-ch] [archive-ch archive-cmd-ch] event-ch]
  (let [cmd-ch (async/chan (async/dropping-buffer 100))
        retry-ch (async/chan (async/sliding-buffer 10000))
        ch (async/chan (async/sliding-buffer 10000))]

    (async/go-loop []
      (let [v (async/<! retry-ch)
            task-id (:subject-id v)]
        (datasets/with-context {:system-state system-state
                                :ctx v}
          [_]
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
                                  :subject-id task-id}))))
        (recur)))

    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (let [propagate-cmd (fn [v]
                                (async/put! result-cmd-ch v)
                                (async/put! archive-cmd-ch v))]
            (condp = (:cmd v)
              :stop
              (do
                (log/info "Detector poll stopped")
                (propagate-cmd v))

              :pause
              (do
                (propagate-cmd v)
                (util/pause cmd-ch #(propagate-cmd %)))

              (do
                (propagate-cmd v)
                (recur))))

          ch
          (datasets/with-context {:system-state system-state
                                  :ctx v}
            [state]
            (let [task-id (:subject-id v)
                  detector-state-ref (datasets/detector-state state detector-state-ref)]
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
                          (let [retries (or (:retries (state/get-task @detector-state-ref task-id)) 0)]
                            (state/set-task-status! detector-state-ref task-id "failed" {:can-retry? true
                                                                                         :retries (inc retries)})))

                        "PROBLEM"
                        (do
                          (async/>! event-ch {:action :poll-task-problem
                                              :subject :task
                                              :subject-id task-id})
                          (log/info "Problem getting results for" task-id)
                          (state/set-task-status! detector-state-ref task-id "failed" {:can-retry? false}))

                        "ARCHIVED"
                        (do
                          (state/archive-task! detector-state-ref task-id)
                          (log/info "Archival successful for task" task-id)
                          (async/>! event-ch {:action :poll-archive-success
                                              :subject :task
                                              :subject-id task-id}))

                        "SUBMITTED"
                        (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) retry-timeout)
                                                            :retries (if-let [r (:retries v)] (inc r) 1))))

                        "RUNNING"
                        (async/go (async/>! retry-ch (assoc v :valid-at (t/plus (t/now) retry-timeout)
                                                            :retries (if-let [r (:retries v)] (inc r) 1))))))
                    (catch Exception e
                      (log/warn "Error while fetching data for" task-id ". " e)))))
              (recur))))))
    [ch cmd-ch]))
