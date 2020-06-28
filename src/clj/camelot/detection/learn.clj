(ns camelot.detection.learn
  (:require
   [camelot.detection.datasets :as datasets]
   [camelot.detection.client :as client]
   [camelot.model.media :as media]
   [camelot.model.sighting :as sighting]
   [camelot.model.suggestion :as suggestion]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(def max-batch-time (* 30 1000))
(def max-batch-size 1000)

(defn- batch [in out max-time max-count]
  (let [lim-1 (dec max-count)]
    (async/go-loop [buf [] t (async/timeout max-time)]
      (let [[v p] (async/alts! [in t])]
        (cond
          (= p t)
          (do
            (async/>! out buf)
            (recur [] (async/timeout max-time)))

          (nil? v)
          (when (seq buf)
            (async/>! out buf))

          (== (count buf) lim-1)
          (do
            (async/>! out (conj buf v))
            (recur [] (async/timeout max-time)))

          :else
          (recur (conj buf v) t))))))

(defn max-confidence
  [v]
  (let [detections (-> v :payload :image :detections)]
    (apply max (map :conf detections))))

(defn build-result-map
  [state media-id]
  (let [media (media/get-specific state media-id)]
    (when (and (:media-detection-completed media) (:media-processed media))
      (let [has-sightings (boolean (seq (sighting/get-all state media-id)))
            suggestions (suggestion/get-all state media-id)
            confs (seq (map :suggestion-confidence
                            (filter #(= (:suggestion-key %) "animal") suggestions)))
            max-conf (if (seq confs) (apply max confs) 0)]
        {:has_animals_actual has-sightings
         :has_animals_detection_max_confidence max-conf}))))

(defn build-result
  [system-state v]
  (datasets/with-context {:system-state system-state
                          :ctx v}
    [state]
    (try
      (->> v
           :media-id
           (build-result-map state))
      (catch Exception e
        (log/warn "Learning failed:" e)))))

(defn- dispatch [system-state ch]
  (async/go-loop []
    (let [vs (async/<! ch)]
      (when (seq vs)
        (let [results (remove nil? (map #(build-result system-state %) vs))]
          (try
            (client/bulk-learn system-state {:results results})
            (log/info "Learning complete for batch of size" (count vs) "/" (count results) "submitted")
            (catch Exception e
              (log/warn "Submitting learning results failed:" e)))))
      (recur))))

(defn run
  "Detection learning."
  [system-state]
  (let [input-ch (async/chan (async/dropping-buffer 50000))
        internal-ch (async/chan (async/dropping-buffer 100))]
    (batch input-ch internal-ch max-batch-time max-batch-size)
    (dispatch system-state internal-ch)
    input-ch))
