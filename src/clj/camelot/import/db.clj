(ns camelot.import.db
  (:require
   [camelot.model.survey :as survey]
   [camelot.model.survey-site :as survey-site]
   [camelot.model.trap-station :as trap-station]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.sighting :as sighting]
   [camelot.model.sighting-field-value :as sighting-field-value]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.site :as site]
   [camelot.model.camera :as camera]
   [camelot.util.data :as data]
   [camelot.model.survey-taxonomy :as survey-taxonomy]))

(defn get-survey
  [state record]
  (->> (:survey-id record)
       (survey/get-specific state)
       (merge record)))

(defn get-or-create-site!
  [state record]
  (->> record
       site/tsite
       (site/get-or-create! state)
       (merge record)))

(defn get-or-create-camera!
  [state record]
  (->> record
       camera/tcamera
       (camera/get-or-create! state)
       (merge record)))

(defn get-or-create-survey-site!
  [state record]
  (->> record
       survey-site/tsurvey-site
       (survey-site/get-or-create! state)
       (merge record)))

(defn get-or-create-trap-station!
  [state record]
  (->> record
       trap-station/ttrap-station
       (trap-station/get-or-create! state)
       (merge record)))

(defn get-or-create-trap-session!
  [state record]
  (->> record
       trap-station-session/ttrap-station-session
       (trap-station-session/get-or-create! state)
       (merge record)))

(defn get-or-create-trap-camera!
  [state record]
  (->> record
       (trap-station-session-camera/ttrap-station-session-camera)
       (trap-station-session-camera/get-or-create-with-camera-and-session! state)
       (merge record)))

(defn get-or-create-taxonomy!
  [state record]
  (if (and (:taxonomy-species record) (:taxonomy-genus record))
    (->> record
         taxonomy/ttaxonomy
         (taxonomy/get-or-create! state)
         (merge record))
    record))

(defn get-or-create-survey-taxonomy!
  [state record]
  (if (and (:taxonomy-id record) (:survey-id record))
    (->> record
         survey-taxonomy/tsurvey-taxonomy
         (survey-taxonomy/get-or-create! state)
         (merge record))
    record))

(defn create-media!
  [state record]
  (->> record
       media/tmedia
       (media/create! state)
       (merge record)))

(defn create-photo!
  [state record]
  (if (and (:photo-resolution-x record)
           (:photo-resolution-y record))
    (->> record
         photo/tphoto
         (photo/create! state)
         (merge record))
    record))

(defn create-sighting!
  [state record]
  (if (and (data/nat? (:sighting-quantity record))
           (data/nat? (:taxonomy-id record)))
    (->> record
         sighting/tsighting
         (sighting/create! state)
         (merge record))
    record))

(defn create-sighting-field-values!
  [state record]
  (when (:sighting-id record)
    ;; Single record may result in many new values, so we don't augment here.
    (sighting-field-value/create-bulk! state
                                       (:sighting-id record)
                                       (:survey-id record)
                                       record)
    record))
