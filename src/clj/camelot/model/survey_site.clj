(ns camelot.model.survey-site
  (:require
   [schema.core :as s]
   [yesql.core :as sql]
   [camelot.model.state :refer [State]]
   [camelot.db :as db]
   [camelot.model.media :as media]))

(sql/defqueries "sql/survey-sites.sql" {:connection db/spec})

(s/defrecord TSurveySite
    [survey-id :- s/Int
     site-id :- s/Int
     site-name :- (s/maybe s/Str)])

(s/defrecord SurveySite
    [survey-site-id :- s/Int
     survey-site-created :- org.joda.time.DateTime
     survey-site-updated :- org.joda.time.DateTime
     survey-id :- s/Int
     site-id :- s/Int
     site-name :- (s/maybe s/Str)])

(s/defn tsurvey-site :- TSurveySite
  [{:keys [survey-id site-id site-name]}]
  (->TSurveySite survey-id site-id site-name))

(s/defn survey-site :- SurveySite
  [{:keys [survey-site-id survey-site-created survey-site-updated survey-id
           site-id site-name]}]
  (->SurveySite survey-site-id survey-site-created survey-site-updated
                survey-id site-id site-name))

(s/defn get-all :- [SurveySite]
  [state :- State
   id :- s/Int]
  (map survey-site (db/with-db-keys state -get-all {:survey-id id})))

(s/defn get-all* :- [SurveySite]
  [state :- State]
  (map survey-site (db/clj-keys (db/with-connection (:connection state) -get-all*))))

(s/defn get-specific :- (s/maybe SurveySite)
  [state :- State
   id :- s/Int]
  (some->>  {:survey-site-id id}
            (db/with-db-keys state -get-specific)
            (first)
            (survey-site)))

(s/defn get-specific-by-site :- (s/maybe SurveySite)
  [state :- State
   data :- TSurveySite]
  (some->> data
           (db/with-db-keys state -get-specific-by-site)
           (first)
           (survey-site)))

(s/defn create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (let [record (db/with-db-keys state -create<! data)]
    (survey-site (get-specific state (int (:1 record))))))

(s/defn update! :- SurveySite
  [state :- State
   id :- s/Int
   data :- TSurveySite]
  (db/with-db-keys state -update! (merge data {:survey-site-id id}))
  (survey-site (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-survey-site state id)]
    (db/with-db-keys state -delete! {:survey-site-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-available
  [state :- State
   id :- s/Int]
  (db/with-db-keys state -get-available {:survey-id id}))

(s/defn get-alternatives
  [state :- State
   id :- s/Int]
  (let [res (get-specific state id)]
    (if res
      (db/with-db-keys state -get-alternatives res)
      [])))

(s/defn get-or-create! :- SurveySite
  [state :- State
   data :- TSurveySite]
  (or (get-specific-by-site state data)
      (create! state data)))
