(ns camelot.model.survey-file
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as sch]
   [camelot.util.db :as db]
   [clojure.java.io :as io]
   [ring.util.response :as r]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]))

(def query (db/with-db-keys :survey-file))

(sch/defrecord TSurveyFile
    [survey-id :- sch/Int
     survey-file-name :- sch/Str
     survey-file-size :- sch/Int]
  {sch/Any sch/Any})

(sch/defrecord SurveyFile
    [survey-id :- sch/Int
     survey-file-id :- sch/Int
     survey-file-name :- sch/Str
     survey-file-size :- sch/Int
     survey-file-created :- org.joda.time.DateTime
     survey-file-updated :- org.joda.time.DateTime]
  {sch/Any sch/Any})

(def survey-file map->SurveyFile)
(def tsurvey-file map->TSurveyFile)

(sch/defn get-all :- [SurveyFile]
  "Retrieve all available files for the given survey."
  [state :- State
   survey-id :- sch/Int]
  (->> {:survey-id survey-id}
       (query state :get-all)
       (map survey-file)))

(sch/defn get-specific :- (sch/maybe SurveyFile)
  "Retrieve the file with the given ID."
  [state :- State
   id :- sch/Int]
  (->> {:survey-file-id id}
       (query state :get-specific)
       (map survey-file)
       first))

(sch/defn get-specific-by-details :- (sch/maybe SurveyFile)
  "Retrieve the file with the given ID."
  [state :- State
   survey-id :- sch/Int
   filename :- sch/Str]
  (->> {:survey-file-name filename
        :survey-id survey-id}
       (query state :get-specific-by-details)
       (map survey-file)
       first))

(sch/defn create! :- SurveyFile
  [state :- State
   data :- TSurveyFile]
  (let [record (query state :create<! data)]
    (survey-file (get-specific state (int (:1 record))))))

(sch/defn update! :- SurveyFile
  [state :- State
   id :- sch/Int
   file-size :- sch/Int]
  (query state :update! {:survey-file-id id
                                   :survey-file-size file-size})
  (survey-file (get-specific state id)))

(sch/defn delete!
  [state :- State
   file-id :- sch/Int]
  (if-let [r (get-specific state file-id)]
    (let [fs (filesystem/filestore-file-path state (:survey-id r)
                                             (:survey-file-name r))]
      (query state :delete! {:survey-file-id file-id})
      (io/delete-file fs))))

(sch/defn upload!
  [state :- State
   survey-id :- sch/Int
   {:keys [tempfile :- sch/Str
           filename :- sch/Str
           size :- sch/Int]}]
  (let [fs (filesystem/filestore-file-path state survey-id filename)
        rec (get-specific-by-details state survey-id filename)]
    (io/copy (file/->file tempfile) (file/->file fs))
    (let [result (if (nil? rec)
                   (create! state (tsurvey-file {:survey-id survey-id
                                                 :survey-file-name filename
                                                 :survey-file-size size}))
                   (update! state (:survey-file-id rec) size))]
      (file/delete tempfile)
      result)))

(sch/defn download
  [state :- State
   file-id :- sch/Int]
  (if-let [r (get-specific state file-id)]
    (let [fs (filesystem/filestore-file-path state (:survey-id r)
                                             (:survey-file-name r))
          data (io/input-stream (file/->file fs))]
      (-> (r/response data)
          (r/header "Content-Length" (file/length (file/->file fs)))
          (r/header "Content-Disposition"
                    (format "attachment; filename=\"%s\"" (:survey-file-name r)))))))
