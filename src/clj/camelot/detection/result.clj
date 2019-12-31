(ns camelot.detection.result
  (:require
   [camelot.services.analytics :as analytics]
   [camelot.detection.state :as state]
   [camelot.model.suggestion :as suggestion]
   [camelot.model.bounding-box :as bounding-box]
   [camelot.util.db :as db]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defn- build-bounding-box
  [detection]
  (bounding-box/tbounding-box
   (apply merge
          {:bounding-box-dimension-type "relative"}
          (map hash-map
               [:bounding-box-min-x
                :bounding-box-min-y
                :bounding-box-width
                :bounding-box-height]
               (:bbox detection)))))

(defn- build-suggestion
  [media-id bb payload detection]
  (suggestion/tsuggestion
   (let [key (get-in payload [:result :detection_categories (:category detection)])]
     (merge (select-keys bb [:bounding-box-id])
            {:suggestion-key key
             :suggestion-label key
             :suggestion-confidence (:conf detection)
             :media-id media-id}))))

(defn run
  "Create suggestions for values placed on the returned channel."
  [state detector-state-ref cmd-mult event-ch]
  (let [cmd-ch (async/chan)
        ch (async/chan)]
    (async/tap cmd-mult cmd-ch)
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector result stopped")
            (recur))

          ch
          (do
            (async/>! event-ch v)
            (log/info "Creating suggestions for media-id" (:subject-id v))
            (let [detections (-> v :payload :image :detections)
                  avg-confidence (/ (reduce + 0 (map :conf detections))
                                    (count detections))]
              (analytics/track state {:category "detector"
                                      :action "result-create-suggestions"
                                      :label "media"
                                      :label-value (:subject-id v)
                                      :dimension1 "suggestions"
                                      :metric1 (count (-> v :payload :image :detections))
                                      :dimension2 "average-confidence"
                                      :metric2 avg-confidence
                                      :ni true}))
            (let [media-id (:subject-id v)]
              (doseq [detection (-> v :payload :image :detections)]
                (log/info "Creating suggestion for media-id" (:subject-id v))
                (try
                  (db/with-transaction [s state]
                    (let [bb (bounding-box/create! state (build-bounding-box detection))]
                      (suggestion/create!
                       state
                       (build-suggestion media-id bb (:payload v) detection))))
                  (catch Exception e
                    (analytics/track state {:category "detector"
                                            :action "result-create-suggestion-failed"
                                            :label "media"
                                            :label-value media-id
                                            :ni true})
                    (log/error "Error while creating suggestion " media-id detection e))))
              (state/set-media-processing-status! detector-state-ref media-id "completed"))
            (recur)))))
    ch))
