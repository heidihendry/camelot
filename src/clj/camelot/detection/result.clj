(ns camelot.detection.result
  (:require [camelot.detection.datasets :as datasets]
            [camelot.detection.state :as state]
            [camelot.detection.util :as util]
            [camelot.model.bounding-box :as bounding-box]
            [camelot.model.suggestion :as suggestion]
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
   (let [key (get-in payload [:detection_categories (keyword (str (:category detection)))])]
     (merge (select-keys bb [:bounding-box-id])
            {:suggestion-key key
             :suggestion-label key
             :suggestion-confidence (:conf detection)
             :media-id media-id}))))

(defn- satisfies-confidence-threshold?
  [state detection]
  (>= (:conf detection)
      (-> state :config :detector :confidence-threshold)))

(defn run
  "Create suggestions for values placed on the returned channel."
  [system-state detector-state-ref event-ch]
  (let [cmd-ch (async/chan (async/dropping-buffer 100))
        ch (async/chan)]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (log/info "Detector result stopped")

            :pause
            (util/pause cmd-ch identity)

            (recur))

          ch
          (datasets/with-context {:system-state system-state
                                  :ctx v}
            [state]
            (let [detector-state-ref (datasets/detector-state state detector-state-ref)]
              (async/>! event-ch v)
              (log/info "Creating suggestions for media-id" (:subject-id v))
              (let [detections (-> v :payload :image :detections)
                    avg-confidence (if (seq detections)
                                     (/ (reduce + 0 (map :conf detections))
                                        (count detections))
                                     0)]
                (async/>! event-ch {:action :result-create-suggestions
                                    :subject :media
                                    :subject-id (:subject-id v)
                                    :meta {:dimension1 "suggestions"
                                           :metric1 (count (-> v :payload :image :detections))
                                           :dimension2 "average-confidence"
                                           :metric2 avg-confidence}}))
              (let [media-id (:subject-id v)
                    detections (-> v :payload :image :detections)]
                (when (some (partial satisfies-confidence-threshold? state) detections)
                  (async/>! event-ch {:action :result-media-with-high-confidence-suggestion
                                      :subject :media
                                      :subject-id media-id}))
                (suggestion/delete-for-media-id! state media-id)
                (doseq [detection detections]
                  (log/info "Creating suggestion for media-id" (:subject-id v))
                  (try
                    (let [bb (bounding-box/create! state (build-bounding-box detection))]
                      (suggestion/create!
                       state
                       (build-suggestion media-id bb (:payload v) detection)))
                    (if (>= (:conf detection)
                            (-> state :config :detector :confidence-threshold))
                      (async/>! event-ch {:action :result-high-confidence-suggestion-added
                                          :subject :media
                                          :subject-id media-id})
                      (async/>! event-ch {:action :result-low-confidence-suggestion-added
                                          :subject :media
                                          :subject-id media-id}))
                    (catch Exception e
                      (async/>! event-ch {:action :result-create-suggestion-failed
                                          :subject :media
                                          :subject-id media-id})
                      (log/error "Error while creating suggestion " media-id detection e))))
                (state/set-media-processing-status! detector-state-ref media-id "completed")))
            (recur)))))
    [ch cmd-ch]))
