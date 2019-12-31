(ns camelot.detection.poll
  (:require
   [camelot.detection.client :as client]
   [camelot.detection.event :as event]
   [camelot.detection.state :as state]
   [clojure.edn :as edn]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]))

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
              (let [delay (max 0 (- (tc/to-long (:valid-at v)) (tc/to-long (t/now))))]
                (log/info "Retrying poll with task id" task-id "in" (/ delay 1000.0) "seconds")
                (async/<! (async/timeout delay))))
            (log/info "Fetching results for task id" task-id)
            (try
              (let [resp (client/get-task state task-id)]
                (condp = (:status resp)
                  "COMPLETED"
                  (do
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

                  ;; TODO distinguish retryable and non-retryable failures
                  "FAILED"
                  (do
                    (log/info "Failed to get results for" task-id)
                    (state/set-task-status! detector-state-ref task-id "failed"))

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
