(ns camelot.model.site
  "Site models and data access."
  (:require
   [schema.core :as s]
   [camelot.system.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :sites))

(s/defrecord TSite
    [site-name :- s/Str
     site-sublocation :- (s/maybe s/Str)
     site-city :- (s/maybe s/Str)
     site-state-province :- (s/maybe s/Str)
     site-country :- (s/maybe s/Str)
     site-area :- (s/maybe s/Num)
     site-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

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
     site-notes :- (s/maybe s/Str)]
  {s/Any s/Any})

(def site map->Site)
(def tsite map->TSite)

(s/defn get-all :- [Site]
  [state :- State]
  (map site (query state :get-all)))

(s/defn get-specific :- (s/maybe Site)
  [state :- State
   id :- s/Int]
  (some->> {:site-id id}
           (query state :get-specific)
           (first)
           (site)))

(s/defn get-specific-by-name :- (s/maybe Site)
  [state :- State
   data :- {:site-name s/Str}]
  (some->> data
           (query state :get-specific-by-name)
           (first)
           (site)))

(s/defn create! :- Site
  [state :- State
   data :- TSite]
  (let [record (query state :create<! data)]
    (site (get-specific state (int (:1 record))))))

(s/defn update! :- Site
  [state :- State
   id :- s/Int
   data :- TSite]
  (query state :update! (merge data {:site-id id}))
  (site (get-specific state id)))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(s/defn delete!
  [state :- State
   id :- s/Int]
  (let [fs (media/get-all-files-by-site state id)
        ps {:site-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/defn get-or-create! :- Site
  [state :- State
   data :- TSite]
  (or (get-specific-by-name state (select-keys data [:site-name]))
      (create! state data)))
