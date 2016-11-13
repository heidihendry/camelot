(ns camelot.util.deployment
  (:require
   [schema.core :as s]
   [camelot.util.data :as data]))

(defn- assoc-cameras-for-group
  [[session-id group]]
  (let [g1 (first group)
        g1' (dissoc g1 :camera-id :camera-name :camera-status-id :camera-media-unrecoverable)
        g2 (second group)]
    (assoc
     (if g2
       (assoc g1'
              :secondary-camera-id (:camera-id g2)
              :secondary-camera-name (:camera-name g2)
              :secondary-camera-status-id (:camera-status-id g2)
              :secondary-camera-media-unrecoverable (:camera-media-unrecoverable g2))
       g1')
     :primary-camera-id (:camera-id g1)
     :primary-camera-name (:camera-name g1)
     :primary-camera-status-id (:camera-status-id g1)
     :primary-camera-media-unrecoverable (:camera-media-unrecoverable g1))))

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
        nd (data/prefix-key d types)
        cams (->> nd
                  (data/select-keys-inv types)
                  vals
                  (filter #(-> % :camera-id data/nat?)))]
    (dissoc (assoc nd :cameras cams) :primary :secondary)))

(s/defn original-camera-removed? :- s/Bool
  "Predicate indicating whether a camera was removed from the next session."
  [active-status-id :- s/Int
   camera]
  (and (data/nat? (:camera-original-id camera))
       (or (not= (:camera-original-id camera) (:camera-id camera))
           (not= (:camera-status-id camera) active-status-id))))

(s/defn camera-active? :- s/Bool
  "Predicate returning true if the camera is/will be considered active."
  [active-status-id :- s/Int
   camera]
  (and (data/nat? (:camera-id camera))
       (or (= (:camera-status-id camera) active-status-id)
           (nil? (:camera-original-id camera))
           (not= (:camera-original-id camera) (:camera-id camera)))))

(s/defn camera-id-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-id")))

(s/defn camera-status-id-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-status-id")))

(s/defn camera-media-unrecoverable-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-media-unrecoverable")))
