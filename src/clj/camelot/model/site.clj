(ns camelot.model.site
  "Site models and data access."
  (:require
   [camelot.spec.model.site :as site-spec]
   [camelot.spec.error :as error-spec]
   [camelot.spec.system :as sysspec]
   [camelot.spec.schema.state :refer [State]]
   [camelot.util.db :as db]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]
   [camelot.spec.cats :as cats-spec]
   [cats.monad.either :as either]
   [schema.core :as sch]
   [clojure.spec.alpha :as s]))

(def query (db/with-db-keys :sites))

(sch/defrecord TSite
    [site-name :- sch/Str
     site-sublocation :- (sch/maybe sch/Str)
     site-city :- (sch/maybe sch/Str)
     site-state-province :- (sch/maybe sch/Str)
     site-country :- (sch/maybe sch/Str)
     site-area :- (sch/maybe sch/Num)
     site-notes :- (sch/maybe sch/Str)]
  {sch/Any sch/Any})

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
        :ret (s/coll-of ::site-spec/site))

(sch/defn get-specific :- (sch/maybe Site)
  [state :- State
   id :- sch/Int]
  (some->> {:site-id id}
           (query state :get-specific)
           (first)
           (site)))

(s/fdef get-specific
        :args (s/cat :state ::sysspec/state :id int?)
        :ret (s/or :not-found nil? :site ::site-spec/site))

(defn get-single
  [state id]
  (if-let [s (get-specific state id)]
    (either/right s)
    (either/left {:error/type :error.type/not-found})))

(s/fdef get-single
        :args (s/cat :state ::sysspec/state :id int?)
        :ret (cats-spec/either? ::error-spec/error
                                ::site-spec/site))

(defn create!
  "Create a site from the passed site data."
  [state data]
  (let [record (query state :create<! data)]
    (site (get-specific state (int (:1 record))))))

(s/fdef create!
        :args (s/cat :state ::sysspec/state :data ::site-spec/tsite)
        :ret ::site-spec/site)

(defn update!
  "Update the site."
  [state id data]
  (query state :update! (merge data {:site-id id}))
  (site (get-specific state id)))

(s/fdef update!
        :args (s/cat :state ::sysspec/state
                     :id int?
                     :data ::site-spec/tsite)
        :ret ::site-spec/site)

(defn patch!
  "Update properties of a site."
  [state id data]
  (if-let [entity (get-specific state id)]
    (either/right (->> data
                       (merge entity)
                       (update! state id)))
    (either/left {:error/type :error.type/not-found})))

(s/fdef patch!
        :args (s/cat :state ::sysspec/state
                     :id int?
                     :data ::site-spec/tsite)
        :ret (cats-spec/either? ::error-spec/error
                                ::site-spec/site))

(defn post!
  [state data]
  (either/right (create! state (tsite data))))

(s/fdef post!
        :args (s/cat :state ::sysspec/state
                     :data ::site-spec/tsite)
        :ret (cats-spec/either? ::error-spec/error
                                ::site-spec/site))

(defn- get-active-cameras
  [state params]
  (->> params
       (query state :get-active-cameras)
       (map :camera-id)
       (remove nil?)))

(defn delete!
  [state id]
  (if-let [site (get-specific state id)]
    (let [fs (media/get-all-files-by-site state id)
          ps {:site-id id}
          cams (get-active-cameras state ps)]
      (query state :delete! ps)
      (media/delete-files! state fs)
      (camera/make-available state cams)
      id)))

(s/fdef delete!
        :args (s/cat :state ::sysspec/state
                     :id int?)
        :ret (s/nilable int?))

(sch/defn mdelete!
  [state :- State
   id :- sch/Int]
  (if (delete! state id)
    (either/right id)
    (either/left {:error/type :error.type/not-found})))

(s/fdef mdelete!
        :args (s/cat :state ::sysspec/state
                     :id int?)
        :ret (cats-spec/either? ::error-spec/error
                                int?))

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
        :args (s/cat :state ::sysspec/state :data ::site-spec/tsite)
        :ret ::site-spec/site)
