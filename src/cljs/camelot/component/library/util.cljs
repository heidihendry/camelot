(ns camelot.component.library.util
  (:require [camelot.state :as state]
            [om.core :as om]
            [camelot.nav :as nav]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]))

(def collection-columns 3)
(def page-size 150)

(defn get-matching
  [data]
  (mapv #(get-in data [:records %])
        (get-in data [:search :ordered-ids])))

(defn media-ids-on-page
  ([data]
   (media-ids-on-page data nil))
  ([data page-num]
   (->> (get-in data [:search :ordered-ids])
        (drop (* (or page-num (- (get-in data [:search :page]) 1)) page-size))
        (take page-size))))

(defn media-on-page
  ([data]
   (mapv #(get-in data [:records %]) (media-ids-on-page data)))
  ([]
   (let [data (state/library-state)]
     (media-on-page data))))

(defn all-media-selected
  ([data]
   (filter :selected (media-on-page data)))
  ([]
   (all-media-selected (state/library-state))))

(defn selection-survey
  ([data]
   (into #{} (map :survey-id) (all-media-selected data)))
  ([]
   (selection-survey (state/library-state))))

(defn survey-sighting-fields
  [survey-id]
  (get-in (state/library-state) [:sighting-fields survey-id]))

(defn find-with-id
  [data media-id]
  (get-in data [:records media-id]))

(defn delete-with-ids!
  [data media-ids]
  (om/transact! data :records (fn [rs] (reduce #(dissoc %1 %2) rs media-ids)))
  (om/transact! data [:search :ordered-ids]
                (fn [ms] (vec (remove #(contains? (into #{} media-ids) %) ms)))))

(defn delete-sightings-from-media-with-id!
  [media-id]
  (om/update! (find-with-id (state/library-state) media-id)
              :sightings []))

(defn select-media-id
  [data media-id]
  (om/update! data [:records media-id :selected] true)
  (om/update! data :selected-media-id media-id)
  (om/update! data :anchor-media-id media-id))

(defn hydrate-media
  [data media md & [cb]]
  (let [start (nav/time-now)]
    (rest/post-x "/library/hydrate" {:data {:media-ids media}}
                 #(do
                    (nav/analytics-timing "library" "hydrate-loaded" (- (nav/time-now) start))
                    (om/update! (state/library-state) :records
                                (->> (:body %)
                                     (reduce (fn [acc x]
                                               (assoc acc (:media-id x)
                                                      (assoc (merge (om/value (get md (:trap-station-session-camera-id x))) x)
                                                             :selected false)))
                                             {})))
                    (select-media-id (state/library-state) (first media))
                    (when cb
                      (cb (state/library-state)))))))

(defn get-media-flags
  [rec]
  (select-keys rec [:media-id
                    :media-attention-needed
                    :media-cameracheck
                    :media-reference-quality
                    :media-processed]))

(defn hide-notification-message
  [msg]
  (om/transact! (state/library-state) :notification
                #(if (= % msg)
                   (assoc % :visible false)
                   %)))

(defn show-notification-message
  [n description]
  (let [msg {:num n
             :description description
             :visible true}]
    (om/update! (state/library-state) :notification msg)
    (js/setTimeout #(hide-notification-message msg) 1600)))

(defn show-select-message
  []
  (let [n (count (all-media-selected))
        action (tr/translate ::selected)]
    (show-notification-message n action)))

(defn show-identified-message
  [n]
  (let [action (tr/translate ::identified)]
    (show-notification-message n action)))

(defn set-flag-states
  [flag-state-map cb]
  (let [selected (all-media-selected)]
    (rest/post-resource "/library/media/flags"
                        {:data (mapv #(merge (get-media-flags %)
                                             flag-state-map)
                                     selected)}
                        (fn []
                          (doall (map
                                  #(doall (map (fn [[k v]] (om/update! % k v)) (vec flag-state-map)))
                                  selected))
                          (cb (count selected))))))

(defn set-attention-needed
  [flag-state]
  (let [action (if flag-state
                (tr/translate ::flagged)
                (tr/translate ::unflagged))
        calc-state (if flag-state
                     {:media-attention-needed flag-state
                      :media-processed false}
                     {:media-attention-needed flag-state})]
    (set-flag-states calc-state
                     #(show-notification-message % action))))

(defn set-reference-quality
  [flag-state]
  (let [action (if flag-state
                 (tr/translate ::reference-quality)
                 (tr/translate ::ordinary-quality))]
    (set-flag-states {:media-reference-quality flag-state}
                     #(show-notification-message % action))))

(defn set-processed
  [flag-state]
  (let [action (if flag-state
                 (tr/translate ::processed)
                 (tr/translate ::unprocessed))]
    (set-flag-states {:media-processed flag-state}
                     #(show-notification-message % action))))

(defn set-cameracheck
  [flag-state]
  (let [action (if flag-state
                 (tr/translate ::test-fires)
                 (tr/translate ::not-test-fires))]
    (set-flag-states (if flag-state
                       {:media-cameracheck true
                        :media-processed true}
                       {:media-cameracheck false})
                     #(show-notification-message % action))))

(defn load-taxonomies
  ([data]
   (rest/get-x "/taxonomy"
               (fn [resp]
                 (om/update! data :species
                             (into {}
                                   (map #(hash-map (get % :taxonomy-id) %)
                                        (:body resp)))))))
  ([data survey-id]
   (rest/get-x (str "/taxonomy/survey/" survey-id)
               (fn [resp]
                 (om/update! data :species
                             (into {}
                                   (map #(hash-map (get % :taxonomy-id) %)
                                        (:body resp))))))))

(defn load-trap-stations
  ([data]
   (rest/get-x "/trap-stations"
               (fn [resp]
                 (om/update! data :trap-stations (:body resp)))))
  ([data survey-id]
   (rest/get-x (str "/trap-stations/survey/" survey-id)
               (fn [resp]
                 (om/update! data :trap-stations (:body resp))))))

(defn deselect-all
  ([data]
   (dorun (map #(om/update! % :selected false) (all-media-selected data))))
  ([]
   (dorun (map #(om/update! % :selected false) (all-media-selected)))))

(defn select-all
  []
  (dorun (map #(om/update! % :selected true) (media-on-page))))

(defn select-all*
  []
  (select-all)
  (show-select-message))

(defn deselect-all*
  []
  (deselect-all)
  (show-select-message))

(defn updated-select-position
  [media-ids e idx]
  (if (nil? idx)
    0
    (case (.-keyCode e)
      37 (do (.preventDefault e)
             (nav/analytics-event "library-key" "<left>")
             (max (- idx 1) 0))
      38 (do (.preventDefault e)
             (nav/analytics-event "library-key" "<up>")
             (if (< idx 3) idx (- idx 3)))
      39 (do (.preventDefault e)
             (nav/analytics-event "library-key" "<right>")
             (min (+ idx 1) (dec (count media-ids))))
      40 (do (.preventDefault e)
             (nav/analytics-event "library-key" "<down>")
             (if (= (.floor js/Math (/ (count media-ids) collection-columns))
                    (.floor js/Math (/ idx collection-columns)))
               idx
               (min (+ idx 3) (dec (count media-ids)))))
      nil)))

(defn apply-selection-range
  [data media-idxs new-endpoint shift ctrl]
  (if (and shift (:anchor-media-id data))
        (let [anchor-idx (ffirst (filter #(= (:anchor-media-id data) (second %)) media-idxs))
              first-idx (min anchor-idx new-endpoint)
              last-idx (max anchor-idx new-endpoint)
              media-in-range (->> media-idxs
                                  (drop first-idx)
                                  (take (inc (- last-idx first-idx)))
                                  (map second)
                                  (map (partial find-with-id data)))]
          (deselect-all data)
          (dorun (map #(om/update! % :selected true) media-in-range))
          (om/update! data :selected-media-id (second (nth media-idxs new-endpoint)))
          (show-select-message))
        (let [id (second (nth media-idxs new-endpoint))]
          (when-not ctrl
            (deselect-all data))
          (if ctrl
            (do
              (om/transact! (find-with-id data id) :selected not)
              (show-select-message))
            (om/update! (find-with-id data id) :selected true))
          (om/update! data :selected-media-id id)
          (om/update! data :anchor-media-id id))))

(defn movement?
  [evt]
  (some? (some #{(.-keyCode evt)}
               [37 38 39 40 65 87 68 83])))

(defn keyboard-select-media
  [data evt]
  (let [media-idxs (vec (map-indexed (fn [i e] [i e]) (media-ids-on-page data)))
        endpoint-idx (ffirst (filter #(= (:selected-media-id data) (second %)) media-idxs))
        new-endpoint (updated-select-position media-idxs evt endpoint-idx)]
    (when (and (movement? evt) new-endpoint)
      (apply-selection-range data media-idxs new-endpoint
                             (.-shiftKey evt) (.-ctrlKey evt)))))

(defn mouse-select-media
  [data media-id shift ctrl]
  (let [media-idxs (vec (map-indexed (fn [i e] [i e]) (media-ids-on-page data)))
        new-endpoint (ffirst (filter #(= media-id (second %)) media-idxs))]
    (when new-endpoint
      (apply-selection-range data media-idxs new-endpoint shift ctrl))))

(defn toggle-select-image
  [data media-id evt]
  (mouse-select-media data media-id
                      (.-shiftKey evt) (.-ctrlKey evt)))

(defn load-library-callback
  [data md resp]
  (let [media-ids (:body resp)]
    (hydrate-media data
                   (take page-size media-ids)
                   md
                   #(do
                      (om/update! % :metadata md)
                      (om/update! (:search %) :ordered-ids (vec media-ids))
                      (om/update! (:search %) :page 1)))))

(defn load-library
  ([data]
   (load-library data ""))
  ([data search]
   (let [start (nav/time-now)]
     (om/update! data [:search :inprogress] true)
     (rest/get-x-opts "/library/metadata"
                      {:success (fn [md]
                                  (rest/post-x-opts "/library" {:search search}
                                                    {:success (fn [resp]
                                                                (nav/analytics-timing "library" "search-loaded" (- (nav/time-now) start))
                                                                (load-library-callback (state/library-state) (:body md) resp))
                                                     :always (fn [x] (om/update!  (state/library-state) [:search :inprogress] false))}))
                       :failure (fn [x] (om/update! data [:search :inprogress] false))}))))
