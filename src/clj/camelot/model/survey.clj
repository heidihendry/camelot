(ns camelot.model.survey
  "Survey data model and persistence."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [yesql.core :as sql]
   [camelot.system.state :refer [State]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]
   [clojure.java.io :as io]
   [camelot.model.media :as media]))

(sql/defqueries "sql/surveys.sql")

(s/defrecord TSurvey
    [survey-name :- s/Str
     survey-sighting-independence-threshold :- s/Num
     survey-directory :- (s/maybe s/Str)
     survey-sampling-point-density :- (s/maybe s/Num)
     survey-notes :- (s/maybe s/Str)
     survey-bulk-import-mode :- (s/maybe s/Bool)]
  {s/Any s/Any})

(s/defrecord Survey
    [survey-id :- s/Int
     survey-created :- org.joda.time.DateTime
     survey-updated :- org.joda.time.DateTime
     survey-name :- s/Str
     survey-sighting-independence-threshold :- s/Num
     survey-directory :- (s/maybe s/Str)
     survey-sampling-point-density :- (s/maybe s/Num)
     survey-notes :- (s/maybe s/Str)
     survey-bulk-import-mode :- (s/maybe s/Bool)]
  {s/Any s/Any})

(defn survey
  [ks]
  (map->Survey (update ks :survey-bulk-import-mode #(or % false))))

(defn tsurvey
  [ks]
  (map->TSurvey (-> ks
                    (update :survey-bulk-import-mode #(or % false))
                    (update :survey-sighting-independence-threshold #(or % 20)))))

(s/defn get-all :- [Survey]
  [state :- State]
  (map survey (db/clj-keys (db/with-connection state -get-all))))

(defn survey-settings
  "Return configuration data for all surveys."
  [state]
  (let [fields (group-by :survey-id (sighting-field/get-all state))]
    (reduce #(let [sid (:survey-id %2)]
               (assoc %1 sid
                      (merge %2 :sighting-fields (or (get fields sid) [])))) {}
            (get-all state))))

(defmacro with-survey-settings
  "Assoc survey settings into a state map."
  [[bind state] & body]
  `(let [~bind (assoc ~state :survey-settings (~#'survey-settings ~state))]
     ~@body))

(s/defn get-specific :- (s/maybe Survey)
  [state :- State
   id :- s/Int]
  (some->> {:survey-id id}
           (db/with-db-keys state -get-specific)
           first
           survey))

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

(s/defn set-bulk-import-mode! :- Survey
  [state :- State
   id :- s/Int
   bulk-import-mode :- s/Bool]
  (db/with-db-keys state -set-bulk-import-mode!
    {:survey-id id
     :survey-bulk-import-mode bulk-import-mode})
  (survey (get-specific state id)))
