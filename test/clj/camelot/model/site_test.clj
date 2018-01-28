(ns camelot.model.site-test
  "Tests around the site model."
  (:require
   [clojure.test.check]
   [clojure.spec.alpha :as s]
   [camelot.system.spec :as sysspec]
   [clojure.spec.test.alpha :as stest]
   [camelot.spec.db :as dbspec]
   [camelot.model.media :as media]
   [camelot.model.camera :as camera]
   [camelot.model.site :as sut]
   [camelot.util.db :as db]
   [camelot.testutil.spec :refer [defspec-test]]
   [camelot.testutil.state :as state]
   [clojure.test :refer :all]))

(s/def ::db-connection (s/keys))

(defn -get-all [& args])
(s/fdef -get-all
        :args (s/cat :data (s/keys)
                     :connection ::db-connection)
        :ret (s/coll-of ::dbspec/site))

(defn -get-specific [& args])
(s/fdef -get-specific
        :args (s/cat :data (s/keys :req-un [::dbspec/site_id])
                     :connection ::db-connection)
        :ret (s/coll-of ::dbspec/site :count 1))

(defn -create<! [& args])
(s/fdef -create<!
        :args (s/cat :data ::dbspec/tsite
                     :connection ::db-connection)
        :ret ::dbspec/execution-result)

(def -update! nil)
(s/fdef -update!
        :args (s/cat :data ::dbspec/tsite-with-id
                     :connection ::db-connection)
        :ret ::dbspec/execution-result)

(defn -delete! [& args])
(s/fdef -delete!
        :args (s/cat :data (s/keys :req-un [::dbspec/site_id])
                     :connection ::db-connection)
        :ret ::dbspec/execution-result)

(defn -get-active-cameras [& args])
(s/fdef -get-active-cameras
        :args (s/cat :data (s/keys)
                     :connection ::db-connection)
        :ret (s/coll-of ::dbspec/camera-ids))

(defn -get-all-files-by-site [& args])
(s/fdef -get-all-files-by-site
        :args (s/cat :data (s/keys :req-un [::dbspec/site_id])
                     :connection ::db-connection)
        :ret (s/coll-of string?))

(defn get-query
  [_ scope qkey]
  (let [state {:sites
               {:get-all -get-all
                :get-specific -get-specific
                :create<! -create<!
                :update! -update!
                :delete! -delete!
                :get-active-cameras -get-active-cameras}
               :media
               {:get-all-files-by-site -get-all-files-by-site}}]
    (get-in state [scope qkey])))

(defn with-instruments [f]
  (stest/instrument `-get-all {:stub #{`-get-all}})
  (stest/instrument `-get-specific {:stub #{`-get-specific}})
  (stest/instrument `-create<! {:stub #{`-create<!}})
  (stest/instrument `-update! {:stub #{`-update!}})
  (stest/instrument `-delete! {:stub #{`-delete!}})
  (stest/instrument `-get-active-cameras {:stub #{`-get-active-cameras}})
  (stest/instrument `db/get-query {:replace {`db/get-query get-query}})
  (stest/instrument `media/delete-files! {:stub #{`media/delete-files!}})
  (stest/instrument `camera/make-available {:stub #{`camera/make-available}})
  (f)
  (stest/unstrument `-get-all)
  (stest/unstrument `-get-specific)
  (stest/unstrument `-create<!)
  (stest/unstrument `-update!)
  (stest/unstrument `-delete!)
  (stest/unstrument `-get-active-cameras)
  (stest/unstrument `db/get-query)
  (stest/unstrument `media/delete-files!)
  (stest/unstrument `camera/make-available))

(use-fixtures :once with-instruments)

(let [opts {:clojure.spec.test.check/opts {:num-tests 10}}]
  (defspec-test testspec-get-all `sut/get-all opts)
  (defspec-test testspec-get-specific `sut/get-specific opts)
  (defspec-test testspec-create! `sut/create! opts)
  (defspec-test testspec-update! `sut/update! opts)
  (defspec-test testspec-delete! `sut/delete! opts))
