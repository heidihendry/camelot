(ns camelot.util.deployment
  (:require
   [camelot.util.data :as data]))

(defn- assoc-cameras-for-group
  [[session-id group]]
  (let [dissoc-list #(apply dissoc %1 %2)
        keys [:camera-id :camera-name :camera-status-id :camera-media-unrecoverable]
        [g1 g2] group]
    (-> g1
        (merge {:primary (select-keys g1 keys)})
        (merge {:secondary (select-keys g2 keys)})
        (data/map-keys-to-key-prefix [:primary :secondary])
        (dissoc-list keys))))

(defn assoc-cameras
  "Associate data for primary and secondary cameras."
  [data]
  (->> data
       (group-by :trap-station-session-id)
       (map assoc-cameras-for-group)))

(defn map-with-cameras-as-list
  "Convert primary and secondary camera data into a list of cameras, stripping the prefix."
  [d]
  (let [types [:primary :secondary]
        nd (data/key-prefix-to-map d types)
        cams (->> nd
                  (data/select-keys-inv types)
                  vals
                  (filter #(-> % :camera-id data/nat?)))]
    (dissoc (assoc nd :cameras cams) :primary :secondary)))

(defn original-camera-removed?
  "Predicate indicating whether a camera was removed from the next session."
  [active-status-id
   camera]
  (and (data/nat? (:camera-original-id camera))
       (or (not= (:camera-original-id camera) (:camera-id camera))
           (not= (:camera-status-id camera) active-status-id))))

(defn camera-active?
  "Predicate returning true if the camera is/will be considered active."
  [active-status-id
   camera]
  (and (data/nat? (:camera-id camera))
       (or (= (:camera-status-id camera) active-status-id)
           (nil? (:camera-original-id camera))
           (not= (:camera-original-id camera) (:camera-id camera)))))

(defn camera-id-key
  [cam-type]
  (keyword (str (name cam-type) "-camera-id")))

(defn camera-status-id-key
  [cam-type]
  (keyword (str (name cam-type) "-camera-status-id")))

(defn camera-media-unrecoverable-key
  [cam-type]
  (keyword (str (name cam-type) "-camera-media-unrecoverable")))
