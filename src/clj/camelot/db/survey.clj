(ns camelot.db.survey
  "Survey data model and persistence."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [yesql.core :as sql]
   [camelot.app.state :refer [State]]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]
   [clojure.java.io :as io]
   [camelot.db.media :as media]))

(sql/defqueries "sql/surveys.sql")

(s/defrecord TSurvey
    [survey-name :- s/Str
     survey-sighting-independence-threshold :- s/Num
     survey-directory :- (s/maybe s/Str)
     survey-sampling-point-density :- (s/maybe s/Num)
     survey-notes :- (s/maybe s/Str)
     survey-bulk-import-mode :- (s/maybe s/Bool)])

(s/defrecord Survey
    [survey-id :- s/Int
     survey-created :- org.joda.time.DateTime
     survey-updated :- org.joda.time.DateTime
     survey-name :- s/Str
     survey-sighting-independence-threshold :- s/Num
     survey-directory :- (s/maybe s/Str)
     survey-sampling-point-density :- (s/maybe s/Num)
     survey-notes :- (s/maybe s/Str)
     survey-bulk-import-mode :- (s/maybe s/Bool)])

(s/defn survey :- Survey
  [{:keys [survey-id survey-created survey-updated survey-name
           survey-sighting-independence-threshold
           survey-directory survey-sampling-point-density
           survey-notes survey-bulk-import-mode]}]
  (->Survey survey-id survey-created survey-updated survey-name
            survey-sighting-independence-threshold survey-directory
            survey-sampling-point-density survey-notes
            (or survey-bulk-import-mode false)))

(s/defn tsurvey :- TSurvey
  [{:keys [survey-name survey-sighting-independence-threshold survey-directory
           survey-sampling-point-density
           survey-notes survey-bulk-import-mode]}]
  (->TSurvey survey-name (or survey-sighting-independence-threshold 20)
             survey-directory survey-sampling-point-density survey-notes
             (or survey-bulk-import-mode false)))

(s/defn get-all :- [Survey]
  [state :- State]
  (map survey (db/clj-keys (db/with-connection state -get-all))))

(s/defn get-specific :- (s/maybe Survey)
  [state :- State
   id :- s/Int]
  (some->> {:survey-id id}
           (db/with-db-keys state -get-specific )
           (first)
           (survey)))

(s/defn create! :- Survey
  [state :- State
   data :- TSurvey]
  (let [record (db/with-db-keys state -create<! data)]
    (survey (get-specific state (int (:1 record))))))

(s/defn update! :- Survey
  [state :- State
   id :- s/Int
   data :- TSurvey]
  (db/with-db-keys state -update! (merge data {:survey-id id}))
  (survey (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-survey state id)]
    (media/delete-files! state fs)
    (file/delete-recursive (filesystem/filestore-survey-directory state id)))
    (db/with-db-keys state -delete! {:survey-id id})
  nil)

(s/defn get-specific-by-name :- (s/maybe Survey)
  [state :- State
   data :- {:survey-name s/Str}]
  (some->> data
           (db/with-db-keys state -get-specific-by-name)
           (first)
           (survey)))

(s/defn get-or-create! :- Survey
  [state :- State
   data :- TSurvey]
  (or (get-specific-by-name state (select-keys data [:survey-name]))
      (create! state data)))
