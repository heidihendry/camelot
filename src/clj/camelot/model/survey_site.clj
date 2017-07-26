(ns camelot.model.survey-site
  (:require
   [schema.core :as s]
   [camelot.system.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :survey-sites))

(s/defrecord TSurveySite
    [survey-id :- s/Int
     site-id :- s/Int
     site-name :- (s/maybe s/Str)]
  {s/Any s/Any})

(s/defrecord SurveySite
    [survey-site-id :- s/Int
     survey-site-created :- org.joda.time.DateTime
     survey-site-updated :- org.joda.time.DateTime
     survey-id :- s/Int
     site-id :- s/Int
     site-name :- (s/maybe s/Str)]
  {s/Any s/Any})

(def survey-site map->SurveySite)
(def tsurvey-site map->TSurveySite)

(s/defn get-all :- [SurveySite]
  [state :- State
   id :- s/Int]
  (map survey-site (query state :get-all {:survey-id id})))

(s/defn get-all* :- [SurveySite]
  [state :- State]
  (map survey-site (query state :get-all*)))

(s/defn get-specific :- (s/maybe SurveySite)
  [state :- State
   id :- s/Int]
  (some->>  {:survey-site-id id}
            (query state :get-specific)
            (first)
            (survey-site)))

(s/defn get-specific-by-site :- (s/maybe SurveySite)
  [state :- State
   data :- TSurveySite]
  (some->> data
           (query state :get-specific-by-site)
           (first)
           (survey-site)))

(s/defn create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (let [record (query state :create<! data)]
    (survey-site (get-specific state (int (:1 record))))))

(s/defn update! :- SurveySite
  [state :- State
   id :- s/Int
   data :- TSurveySite]
  (query state :update! (merge data {:survey-site-id id}))
  (survey-site (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-survey-site state id)
        ps {:survey-site-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/defn get-available
  [state :- State
   id :- s/Int]
  (query state :get-available {:survey-id id}))

(s/defn get-alternatives
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (if res
      (query state :get-alternatives res)
      [])))

(s/defn get-or-create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (or (get-specific-by-site state data)
      (create! state data)))
