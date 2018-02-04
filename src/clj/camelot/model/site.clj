(ns camelot.model.site
  "Site models and data access."
  (:require
   [schema.core :as sch]
   [camelot.spec.system :as sysspec]
   [clojure.spec.alpha :as s]
   [clj-time.spec :as tspec]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]))

(def query (db/with-db-keys :sites))

(s/def ::site-id int?)
(s/def ::site-created ::tspec/date-time)
(s/def ::site-updated ::tspec/date-time)
(s/def ::site-name string?)
(s/def ::site-sublocation (s/nilable string?))
(s/def ::site-city (s/nilable string?))
(s/def ::site-state-province (s/nilable string?))
(s/def ::site-country (s/nilable string?))
(s/def ::site-area (s/nilable number?))
(s/def ::site-notes (s/nilable string?))

(s/def ::tsite
  (s/keys :req-un [::site-name]
          :opt-un [::site-sublocation
                   ::site-city
                   ::site-state-province
                   ::site-country
                   ::site-area
                   ::site-notes]))

(sch/defrecord TSite
    [site-name :- sch/Str
     site-sublocation :- (sch/maybe sch/Str)
     site-city :- (sch/maybe sch/Str)
     site-state-province :- (sch/maybe sch/Str)
     site-country :- (sch/maybe sch/Str)
     site-area :- (sch/maybe sch/Num)
     site-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(s/def ::site
  (s/keys :req-un [::site-id
                   ::site-created
                   ::site-updated
                   ::site-name]
          :opt-un [::site-sublocation
                   ::site-city
                   ::site-state-province
                   ::site-country
                   ::site-area
                   ::site-notes]))

(sch/defrecord Site
    [site-id :- sch/Int
     site-created :- org.joda.time.DateTime
     site-updated :- org.joda.time.DateTime
     site-name :- sch/Str
     site-sublocation :- (sch/maybe sch/Str)
     site-city :- (sch/maybe sch/Str)
     site-state-province :- (sch/maybe sch/Str)
     site-country :- (sch/maybe sch/Str)
     site-area :- (sch/maybe sch/Num)
     site-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

(def site map->Site)
(def tsite map->TSite)

(sch/defn get-all :- [Site]
  "Retrieve all sites."
  [state :- State]
  (map site (query state :get-all)))

(s/fdef get-all
        :args (s/cat :state ::sysspec/state)
        :ret (s/coll-of ::site))

(sch/defn get-specific :- (sch/maybe Site)
  [state :- State
   id :- sch/Int]
  (some->> {:site-id id}
           (query state :get-specific)
           (first)
           (site)))

(s/fdef get-specific
        :args (s/cat :state ::sysspec/state :id int?)
        :ret (s/or :not-found nil? :site ::site))

(sch/defn create! :- Site
  "Create a site from the passed site data."
  [state :- State
   data :- TSite]
  (let [record (query state :create<! data)]
    (site (get-specific state (int (:1 record))))))

(s/fdef create!
        :args (s/cat :state ::sysspec/state :data ::tsite)
        :ret ::site)

(sch/defn update! :- Site
  "Update the site."
  [state :- State
   id :- sch/Int
   data :- TSite]
  (query state :update! (merge data {:site-id id}))
  (site (get-specific state id)))

(s/fdef update!
        :args (s/cat :state ::sysspec/state
                     :id int?
                     :data ::tsite)
        :ret ::site)

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(sch/defn delete!
  [state :- State
   id :- sch/Int]
  (let [fs (media/get-all-files-by-site state id)
        ps {:site-id id}
        cams (get-active-cameras state ps)]
    (query state :delete! ps)
    (media/delete-files! state fs)
    (camera/make-available state cams))
  nil)

(s/fdef delete!
        :args (s/cat :state ::sysspec/state
                     :id int?)
        :ret nil?)

(defn- get-specific-by-name
  [state data]
  (some->> data
           (query state :get-specific-by-name)
           (first)
           (site)))

(sch/defn get-or-create! :- Site
  [state :- State
   data :- TSite]
  (or (get-specific-by-name state (select-keys data [:site-name]))
      (create! state data)))

(s/fdef get-or-create!
        :args (s/cat :state ::sysspec/state :data ::tsite)
        :ret ::site)
