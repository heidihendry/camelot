(ns camelot.model.site
  (:require [schema.core :as s]
            [yesql.core :as sql]
            [camelot.model.state :refer [State]]
            [camelot.db :as db]
            [camelot.model.media :as media]))

(sql/defqueries "sql/sites.sql" {:connection db/spec})

(s/defrecord TSite
    [site-name :- s/Str
     site-sublocation :- (s/maybe s/Str)
     site-city :- (s/maybe s/Str)
     site-state-province :- (s/maybe s/Str)
     site-country :- (s/maybe s/Str)
     site-area :- (s/maybe s/Num)
     site-notes :- (s/maybe s/Str)])

(s/defrecord Site
    [site-id :- s/Int
     site-created :- org.joda.time.DateTime
     site-updated :- org.joda.time.DateTime
     site-name :- s/Str
     site-sublocation :- (s/maybe s/Str)
     site-city :- (s/maybe s/Str)
     site-state-province :- (s/maybe s/Str)
     site-country :- (s/maybe s/Str)
     site-area :- (s/maybe s/Num)
     site-notes :- (s/maybe s/Str)])

(s/defn tsite :- TSite
  [{:keys [site-name site-sublocation site-city site-state-province
           site-country site-area site-notes]}]
  (->TSite site-name site-sublocation site-city site-state-province
           site-country site-area site-notes))

(s/defn site :- Site
  [{:keys [site-id site-created site-updated site-name site-sublocation
           site-city site-state-province site-country site-area site-notes]}]
  (->Site site-id site-created site-updated site-name site-sublocation
          site-city site-state-province site-country site-area site-notes))

(s/defn get-all :- [Site]
  [state :- State]
  (map site (db/clj-keys (db/with-connection (:connection state) -get-all))))

(s/defn get-specific :- (s/maybe Site)
  [state :- State
   id :- s/Int]
  (some->> {:site-id id}
           (db/with-db-keys state -get-specific)
           (first)
           (site)))

(s/defn get-specific-by-name :- (s/maybe Site)
  [state :- State
   data :- {:site-name s/Str}]
  (some->> data
           (db/with-db-keys state -get-specific-by-name)
           (first)
           (site)))

(s/defn create! :- Site
  [state :- State
   data :- TSite]
  (let [record (db/with-db-keys state -create<! data)]
    (site (get-specific state (int (:1 record))))))

(s/defn update! :- Site
  [state :- State
   id :- s/Int
   data :- TSite]
  (db/with-db-keys state -update! (merge data {:site-id id}))
  (site (get-specific state id)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-site state id)]
    (db/with-db-keys state -delete! {:site-id id})
    (media/delete-files! state fs))
  nil)

(s/defn get-or-create! :- Site
  [state :- State
   data :- TSite]
  (or (get-specific-by-name state (select-keys data [:site-name]))
      (create! state data)))
