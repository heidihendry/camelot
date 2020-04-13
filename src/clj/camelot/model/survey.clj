(ns camelot.model.survey
  "Survey data model and persistence."
  (:require
   [cats.monad.either :as either]
   [schema.core :as sch]
   [camelot.util.db :as db]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.sighting-field :as sighting-field]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]
   [camelot.model.trap-station :as trap-station]))

(def query (db/with-db-keys :surveys))

(sch/defrecord TSurvey
    [survey-name :- sch/Str
     survey-sighting-independence-threshold :- sch/Num
     survey-directory :- (sch/maybe sch/Str)
     survey-sampling-point-density :- (sch/maybe sch/Num)
     survey-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(sch/defrecord Survey
    [survey-id :- sch/Int
     survey-created :- org.joda.time.DateTime
     survey-updated :- org.joda.time.DateTime
     survey-name :- sch/Str
     survey-sighting-independence-threshold :- sch/Num
     survey-directory :- (sch/maybe sch/Str)
     survey-sampling-point-density :- (sch/maybe sch/Num)
     survey-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(defn survey
  [ks]
  (map->Survey ks))

(defn tsurvey
  [ks]
  (map->TSurvey (update ks :survey-sighting-independence-threshold #(or % 20))))

(sch/defn get-all :- [Survey]
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

(defn assoc-bulk-import-available
  [state survey]
  (let [ts (trap-station/get-all-for-survey state (:survey-id survey))]
    (assoc survey :survey-bulk-import-available (empty? ts))))

(defn get-specific
  [state id]
  (some->> {:survey-id id}
           (query state :get-specific)
           first
           (assoc-bulk-import-available state)
           survey))

(defn get-single
  [state id]
  (if-let [s (get-specific state id)]
    (either/right s)
    (either/left {:error/type :error.type/not-found})))

(defn get-specific-by-name
  [state data]
  (some->> data
           (query state :get-specific-by-name)
           first
           survey))

(sch/defn create! :- Survey
  [state :- State
   data :- TSurvey]
  (let [record (query state :create<! data)
        s (survey (get-specific state (int (:1 record))))]
    (sighting-field/create-default-fields! state (:survey-id s))
    s))

(defn update!
  [state id data]
  (query state :update! (merge data {:survey-id id}))
  (survey (get-specific state id)))

(defn patch!
  [state id data]
  (if-let [entity (get-specific state id)]
    (let [named (get-specific-by-name state (select-keys data [:survey-name]))]
      (if (and named (not= (:survey-id named) id))
        (either/left {:error/type :error.type/conflict})
        (either/right (->> data
                           (merge entity)
                           tsurvey
                           (update! state id)))))
    (either/left {:error/type :error.type/not-found})))

(defn post!
  [state data]
  (if (get-specific-by-name state (select-keys data [:survey-name]))
    (either/left {:error/type :error.type/conflict})
    (either/right (create! state (tsurvey data)))))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(defn delete!
  [state id]
  (when (get-specific state id)
    (let [fs (media/get-all-files-by-survey state id)
          ps {:survey-id id}
          cams (get-active-cameras state ps)]
      (media/delete-files! state fs)
      (file/delete-recursive (filesystem/filestore-survey-directory state id))
      (camera/make-available state cams)
      (query state :delete! ps)
      id)))

(defn mdelete!
  [state id]
  (if (delete! state id)
    (either/right id)
    (either/left {:error/type :error.type/not-found})))

(sch/defn get-or-create! :- Survey
  [state :- State
   data :- TSurvey]
  (or (get-specific-by-name state (select-keys data [:survey-name]))
      (create! state data)))
