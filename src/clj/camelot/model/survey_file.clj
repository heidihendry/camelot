(ns camelot.model.survey-file
  (:require
   [camelot.spec.schema.state :refer [State]]
   [schema.core :as s]
   [camelot.util.db :as db]
   [camelot.translation.core :as tr]
   [clojure.java.io :as io]
   [ring.util.response :as r]
   [camelot.util.file :as file]
   [camelot.util.filesystem :as filesystem]))

(def query (db/with-db-keys :survey-file))

(s/defrecord TSurveyFile
    [survey-id :- s/Int
     survey-file-name :- s/Str
     survey-file-size :- s/Int]
  {s/Any s/Any})

(s/defrecord SurveyFile
    [survey-id :- s/Int
     survey-file-id :- s/Int
     survey-file-name :- s/Str
     survey-file-size :- s/Int
     survey-file-created :- org.joda.time.DateTime
     survey-file-updated :- org.joda.time.DateTime]
  {s/Any s/Any})

(def survey-file map->SurveyFile)
(def tsurvey-file map->TSurveyFile)

(s/defn get-all :- [SurveyFile]
  "Retrieve all available files for the given survey."
  [state :- State
   survey-id :- s/Int]
  (->> {:survey-id survey-id}
       (query state :get-all)
       (map survey-file)))

(s/defn get-specific :- (s/maybe SurveyFile)
  "Retrieve the file with the given ID."
  [state :- State
   id :- s/Int]
  (->> {:survey-file-id id}
       (query state :get-specific)
       (map survey-file)
       first))

(s/defn get-specific-by-details :- (s/maybe SurveyFile)
  "Retrieve the file with the given ID."
  [state :- State
   survey-id :- s/Int
   filename :- s/Str]
  (->> {:survey-file-name filename
        :survey-id survey-id}
       (query state :get-specific-by-details)
       (map survey-file)
       first))

(s/defn create! :- SurveyFile
  [state :- State
   data :- TSurveyFile]
  (let [record (query state :create<! data)]
    (survey-file (get-specific state (int (:1 record))))))

(s/defn update! :- SurveyFile
  [state :- State
   id :- s/Int
   file-size :- s/Int]
  (query state :update! {:survey-file-id id
                                   :survey-file-size file-size})
  (survey-file (get-specific state id)))

(s/defn delete!
  [state :- State
   file-id :- s/Int]
  (if-let [r (get-specific state file-id)]
    (let [fs (filesystem/filestore-file-path state (:survey-id r)
                                             (:survey-file-name r))]
      (query state :delete! {:survey-file-id file-id})
      (io/delete-file fs))))

(s/defn upload!
  [state :- State
   survey-id :- s/Int
   {:keys [tempfile :- s/Str
           filename :- s/Str
           size :- s/Int]}]
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

(s/defn download
  [state :- State
   file-id :- s/Int]
  (if-let [r (get-specific state file-id)]
    (let [fs (filesystem/filestore-file-path state (:survey-id r)
                                             (:survey-file-name r))
          data (io/input-stream (file/->file fs))]
      (-> (r/response data)
          (r/header "Content-Length" (file/length (file/->file fs)))
          (r/header "Content-Disposition"
                    (format "attachment; filename=\"%s\"" (:survey-file-name r)))))))
