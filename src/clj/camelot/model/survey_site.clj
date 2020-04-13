(ns camelot.model.survey-site
  (:require
   [schema.core :as sch]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :survey-sites))

(sch/defrecord TSurveySite
    [survey-id :- sch/Int
     site-id :- sch/Int
     site-name :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(sch/defrecord SurveySite
    [survey-site-id :- sch/Int
     survey-site-created :- org.joda.time.DateTime
     survey-site-updated :- org.joda.time.DateTime
     survey-id :- sch/Int
     site-id :- sch/Int
     survey-name :- (sch/maybe sch/Str)
     site-name :- (sch/maybe sch/Str)
     survey-site-label :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(defn- format-survey-site-label
  [data]
  (let [survey-name (:survey-name data)
        site-name (:site-name data)]
    (format "%s - %s" survey-name site-name)))

(defn survey-site
  [data]
  (let [label (format-survey-site-label data)]
    (-> data
        (assoc :survey-site-label label)
        map->SurveySite)))

(def tsurvey-site map->TSurveySite)

(sch/defn get-all :- [SurveySite]
  [state :- State
   id :- sch/Int]
  (map survey-site (query state :get-all {:survey-id id})))

(sch/defn get-all* :- [SurveySite]
  [state :- State]
  (map survey-site (query state :get-all*)))

(sch/defn get-specific :- (sch/maybe SurveySite)
  [state :- State
   id :- sch/Int]
  (some->>  {:survey-site-id id}
            (query state :get-specific)
            (first)
            (survey-site)))

(sch/defn get-specific-by-site :- (sch/maybe SurveySite)
  [state :- State
   data :- TSurveySite]
  (some->> data
           (query state :get-specific-by-site)
           (first)
           (survey-site)))

(sch/defn create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (let [record (query state :create<! data)]
    (survey-site (get-specific state (int (:1 record))))))

(sch/defn update! :- SurveySite
  [state :- State
   id :- sch/Int
   data :- TSurveySite]
  (query state :update! (merge data {:survey-site-id id}))
  (survey-site (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-survey-site state id)
        ps {:survey-site-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(sch/defn get-available
  [state :- State
   id :- sch/Int]
  (query state :get-available {:survey-id id}))

(sch/defn get-alternatives
  [state :- State
   id :- sch/Int]
  (let [res (get-specific state id)]
    (if res
      (query state :get-alternatives res)
      [])))

(sch/defn get-or-create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (or (get-specific-by-site state data)
      (create! state data)))
