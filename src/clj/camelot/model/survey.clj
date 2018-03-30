(ns camelot.model.survey
  "Survey data model and persistence."
  (:require
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]
   [clojure.java.io :as io]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :surveys))

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
  (map survey (query state :get-all)))

(defn survey-settings
  "Return configuration data for all surveys."
  [state]
  (let [fields (group-by :survey-id (sighting-field/get-all state))]
    (reduce #(let [sid (:survey-id %2)]
               (assoc %1 sid
                      (assoc %2 :sighting-fields (or (get fields sid) [])))) {}
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
           (query state :get-specific)
           first
           survey))

(s/defn create! :- Survey
  [state :- State
   data :- TSurvey]
  (let [record (query state :create<! data)
        s (survey (get-specific state (int (:1 record))))]
    (sighting-field/create-default-fields! state (:survey-id s))
    s))

(s/defn update! :- Survey
  [state :- State
   id :- s/Int
   data :- TSurvey]
  (query state :update! (merge data {:survey-id id}))
  (survey (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-survey state id)
        ps {:survey-id id}
        cams (get-active-cameras state ps)]
    (media/delete-files! state fs)
    (file/delete-recursive (filesystem/filestore-survey-directory state id))
    (camera/make-available state cams)
    (query state :delete! ps))
  nil)

(s/defn get-specific-by-name :- (s/maybe Survey)
  [state :- State
   data :- {:survey-name s/Str}]
  (some->> data
           (query state :get-specific-by-name)
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
  (query state :set-bulk-import-mode!
    {:survey-id id
     :survey-bulk-import-mode bulk-import-mode})
  (survey (get-specific state id)))
