(ns camelot.component.library.util
  (:require [camelot.state :as state]
            [om.core :as om]
            [camelot.rest :as rest]
            [camelot.util.filter :as filter]))

(defn get-matching
  [data]
  (let [search (:search data)]
    (map #(get-in search [:results %]) (:matches search))))

(defn all-media-selected
  []
  (filter :selected (get-matching (state/library-state))))

(defn find-with-id
  [media-id]
  (get-in (state/library-state) [:search :results media-id]))

(defn load-library-callback
  [resp]
  (om/update! (state/library-state) :selected-media-id nil)
  (om/update! (get (state/library-state) :search) :results
              (reduce-kv (fn [acc k v] (assoc acc k (first v))) {}
                         (group-by :media-id (:body resp))))
  (om/update! (:search (state/library-state)) :page 1)
  (om/update! (:search (state/library-state)) :matches
              (map :media-id (filter/only-matching (get-in (state/library-state) [:search :terms])
                                            (state/library-state)))))

(defn get-media-flags
  [rec]
  (select-keys rec [:media-id
                    :media-attention-needed
                    :media-processed]))

(defn set-flag-state
  [flag-key flag-state]
  (let [selected (all-media-selected)]
    (rest/post-resource "/library/media/flags"
                        {:data (mapv #(assoc (get-media-flags %)
                                             flag-key flag-state)
                                     selected)}
                        (fn []
                          (doall (map
                                  #(om/update! % flag-key flag-state)
                                  selected))))))

(defn set-attention-needed
  [flag-state]
  (set-flag-state :media-attention-needed flag-state))

(defn set-processed
  [flag-state]
  (set-flag-state :media-processed flag-state))

(defn load-library
  ([]
   (rest/get-x "/library" load-library-callback))
  ([survey-id]
   (rest/get-x (str "/library/" survey-id) load-library-callback)))

(defn load-trap-stations
  ([]
   (rest/get-x "/trap-stations"
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp)))))
  ([survey-id]
   (rest/get-x (str "/trap-stations/survey/" survey-id)
               (fn [resp]
                 (om/update! (state/library-state) :trap-stations (:body resp))))))

(def page-size 50)

(defn hide-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count dec))

(defn show-select-message
  []
  (om/transact! (:search (state/library-state)) :show-select-count inc)
  (.setTimeout js/window hide-select-message 1600))

(defn deselect-all
  []
  (dorun (map #(om/update! % :selected false) (all-media-selected))))

(defn media-on-page
  ([data] (let [data (state/library-state)]
            (vec (take page-size
                       (drop (* (- (get-in data [:search :page]) 1) page-size)
                             (get-matching data))))))
  ([]
   (let [data (state/library-state)]
     (vec (take page-size
                (drop (* (- (get-in data [:search :page]) 1) page-size)
                      (get-matching data)))))))

(defn select-all
  []
  (dorun (map #(om/update! % :selected true) (media-on-page))))

(defn select-all*
  []
  (show-select-message)
  (select-all))

(defn deselect-all*
  []
  (show-select-message)
  (deselect-all))

(defn toggle-select-image
  [data ctrl]
  (if (and ctrl (:selected data))
    (om/update! (state/library-state) :selected-media-id nil)
    (om/update! (state/library-state) :selected-media-id (:media-id data)))
  (when (not ctrl)
    (deselect-all))
  (om/transact! data :selected not)
  (show-select-message))

