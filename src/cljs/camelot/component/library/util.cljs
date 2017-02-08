(ns camelot.component.library.util
  (:require [camelot.state :as state]
            [om.core :as om]
            [camelot.rest :as rest]
            [camelot.util.filter :as filter]
            [camelot.translation.core :as tr]))

(def page-size 50)

(defn get-matching
  [data]
  (let [search (:search data)]
    (map #(get-in search [:results %]) (get-in data [:search-results :all-ids]))))

(defn media-ids-on-page
  [data]
  (->> (get-in data [:search-results :all-ids])
       (drop (* (- (get-in data [:search :page]) 1) page-size))
       (take page-size)))

(defn media-on-page
  ([data]
   (mapv #(get-in data [:search :results %]) (media-ids-on-page data)))
  ([]
   (let [data (state/library-state)]
     (media-on-page data))))

(defn all-media-selected
  []
  (filter :selected (media-on-page (state/library-state))))

(defn find-with-id
  [media-id]
  (get-in (state/library-state) [:search :results media-id]))

(defn delete-with-id!
  [media-id]
  (om/transact! (state/library-state) [:search :results] #(dissoc % media-id))
  (om/transact! (state/library-state) [:search-results :all-ids]
                (fn [ms] (remove #(= media-id %) ms))))

(defn load-library-callback
  [resp]
  (om/update! (state/library-state) :selected-media-id nil)
  (om/update! (get (state/library-state) :search) :results
              (reduce (fn [acc v] (assoc acc (:media-id v) v)) {}
                      (:body resp)))
  (let [mid (mapv :media-id (:body resp))]
    (om/update! (get (state/library-state) :search) :ordered-ids mid)
    (om/update! (get (state/library-state) :search-results) :all-ids mid))
  (om/update! (:search (state/library-state)) :page 1)
  (om/update! (:search (state/library-state)) :dirty-state true))

(defn get-media-flags
  [rec]
  (select-keys rec [:media-id
                    :media-attention-needed
                    :media-cameracheck
                    :media-reference-quality
                    :media-processed]))

(defn hide-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count dec)
  (om/update! (state/library-state) [:search :show-select-count-override] false))

(defn show-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (.setTimeout js/window hide-select-message 1600))

(defn show-identified-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (om/update! (state/library-state) [:search :show-select-action] (tr/translate ::identified))
  (om/update! (state/library-state) [:search :show-select-count-override] true)
  (.setTimeout js/window hide-select-message 1600))

(defn set-flag-states
  [flag-state-map]
  (let [selected (all-media-selected)]
    (rest/post-resource "/library/media/flags"
                        {:data (mapv #(merge (get-media-flags %)
                                             flag-state-map)
                                     selected)}
                        (fn []
                          (doall (map
                                  #(doall (map (fn [[k v]] (om/update! % k v)) (vec flag-state-map)))
                                  selected))))))

(defn set-attention-needed
  [flag-state]
  (om/update! (:search (state/library-state)) :show-select-action
              (if flag-state
                (tr/translate ::flagged)
                (tr/translate ::unflagged)))
  (show-select-message)
  (let [calc-state (if flag-state
                     {:media-attention-needed flag-state
                      :media-processed false}
                     {:media-attention-needed flag-state})]
    (set-flag-states calc-state)))

(defn set-reference-quality
  [flag-state]
  (om/update! (:search (state/library-state))
              :show-select-action (if flag-state
                                    (tr/translate ::reference-quality)
                                    (tr/translate ::ordinary-quality)))
  (show-select-message)
  (set-flag-states {:media-reference-quality flag-state}))

(defn set-processed
  [flag-state]
  (om/update! (:search (state/library-state)) :show-select-action
              (if flag-state
                (tr/translate ::processed)
                (tr/translate ::unprocessed)))
  (show-select-message)
  (set-flag-states {:media-processed flag-state}))

(defn set-cameracheck
  [flag-state]
  (om/update! (:search (state/library-state)) :show-select-action
              (if flag-state
                (tr/translate ::test-fires)
                (tr/translate ::not-test-fires)))
  (show-select-message)
  (set-flag-states (if flag-state
                     {:media-cameracheck true
                      :media-processed true}
                     {:media-cameracheck false})))

(defn load-library
  ([]
   (rest/get-x "/library" load-library-callback))
  ([survey-id]
   (rest/get-x (str "/library/" survey-id) load-library-callback)))

(defn load-taxonomies
  ([]
   (rest/get-x "/taxonomy"
               (fn [resp]
                 (om/update! (state/library-state) :species
                             (into {}
                                   (map #(hash-map (get % :taxonomy-id) %)
                                        (:body resp)))))))
  ([survey-id]
   (rest/get-x (str "/taxonomy/survey/" survey-id)
               (fn [resp]
                 (om/update! (state/library-state) :species
                             (into {}
                                   (map #(hash-map (get % :taxonomy-id) %)
                                        (:body resp))))))))

(defn load-trap-stations
  ([]
   (rest/get-x "/trap-stations"
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp)))))
  ([survey-id]
   (rest/get-x (str "/trap-stations/survey/" survey-id)
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp))))))

(defn deselect-all
  []
  (dorun (map #(om/update! % :selected false) (all-media-selected))))

(defn select-all
  []
  (dorun (map #(om/update! % :selected true) (media-on-page))))

(defn select-all*
  []
  (om/update! (:search (state/library-state)) :show-select-action
              (tr/translate ::selected))
  (show-select-message)
  (select-all))

(defn deselect-all*
  []
  (om/update! (:search (state/library-state)) :show-select-action
              (tr/translate ::selected))
  (show-select-message)
  (deselect-all))

(defn toggle-select-image
  [data ctrl]
  (if (and ctrl (:selected data))
    (do
      (om/update! (state/library-state) :selected-media-id
                  (:media-id (first (all-media-selected))))
      (if (= (:media-id data) (:anchor-media-id (state/library-state)))
        (om/update! (state/library-state) :anchor-media-id nil)
        (om/update! (state/library-state) :anchor-media-id (:media-id data))))
    (do
      (om/update! (state/library-state) :anchor-media-id (:media-id data))
      (om/update! (state/library-state) :selected-media-id (:media-id data))))
  (when (not ctrl)
    (deselect-all))
  (om/transact! data :selected not)
  (om/update! (:search (state/library-state)) :show-select-action
              (tr/translate ::selected))
  (show-select-message))
