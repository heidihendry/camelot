(ns camelot.system.datasets-test
  (:require
   [camelot.system.protocols :as protocols]
   [camelot.system.datasets :as sut]
   [camelot.testutil.mock :refer [defmock with-spies]]
   [clojure.test :as t]))

(defrecord MockDatabase [connect-fn disconnect-fn]
  protocols/Connectable
  (connect [_ id]
    (connect-fn id))
  (disconnect [_ id]
    (disconnect-fn id)))

(defrecord MockMigrater []
  protocols/Migratable
  (migrate [this x] nil)
  (rollback [this x] nil))

(def ^:private config
  {:datasets {:default {:paths {:database "/path/to/Database"}}
              :special {:paths {:database "/special/Database"}}}})

(t/deftest test-datasets-record
  (t/testing "Datasets"
    (t/testing "start"
      (t/testing "invokes Database#connect"
        (t/testing "for a single dataset"
          (with-spies [calls]
            (let [connect (defmock [_] nil)]
              (.start (sut/map->Datasets {:config {:datasets {:default {:paths {:database "/path/to/Database"}}}}
                                          :migrater (->MockMigrater)
                                          :database (map->MockDatabase
                                                     {:connect-fn connect
                                                      :disconnect-fn identity})}))
              (t/is (= ["/path/to/Database"] (first (get (calls) connect))))
              (t/is (= 1 (count (get (calls) connect)))))))

        (t/testing "for a all datasets present"
          (with-spies [calls]
            (let [connect (defmock [_] nil)
                  disconnect identity]
              (.start (sut/map->Datasets {:config config
                                          :migrater (->MockMigrater)
                                          :database (map->MockDatabase
                                                     {:connect-fn connect
                                                      :disconnect-fn disconnect})}))
              (t/is (= #{["/path/to/Database"]
                         ["/special/Database"]} (set (get (calls) connect))))
              (t/is (= 2 (count (get (calls) connect))))))))

      (t/testing "inspect"
        (t/testing "returns expected result if able to connect to at least one database"
          (let [connect identity
                datasets (sut/map->Datasets {:config config
                                             :migrater (->MockMigrater)
                                             :database (map->MockDatabase
                                                        {:connect-fn connect
                                                         :disconnect-fn identity})})]
            (t/is (= #{:default :special} (.inspect (.start datasets))))))

        (t/testing "throws if unable to connect to any databases"
          (let [connect #(throw (ex-info "Throws" {}))
                deps {:config {:datasets config}
                      :migrater (->MockMigrater)
                      :database (map->MockDatabase
                                 {:connect-fn connect
                                  :disconnect-fn identity})}
                datasets (sut/map->Datasets deps)]
            (t/is (thrown? RuntimeException (.start datasets)))))

        (t/testing "returns only the databases to which it can connect"
          (let [connect (fn [database]
                          (if (= "/special/Database" database)
                            (throw (ex-info "Throws" {}))
                            nil))
                deps {:config config
                      :migrater (->MockMigrater)
                      :database (map->MockDatabase
                                 {:connect-fn connect
                                  :disconnect-fn identity})}
                datasets (sut/map->Datasets deps)]
            (t/is (= #{:default} (.inspect (.start datasets)))))))

      (t/testing "stop"
        (t/testing "invokes Database#disconnect"
          (t/testing "for all databases"
            (with-spies [calls]
              (let [disconnect (defmock [_] nil)
                    datasets (sut/map->Datasets {:config config
                                                 :migrater (->MockMigrater)
                                                 :database (map->MockDatabase
                                                            {:connect-fn identity
                                                             :disconnect-fn disconnect})})]
                (.stop (.start datasets))
                (t/is (= #{["/path/to/Database"]
                           ["/special/Database"]} (set (get (calls) disconnect))))
                (t/is (= 2 (count (get (calls) disconnect))))))))

        (t/testing "returns the expected object"
          (let [datasets (sut/map->Datasets {:config config
                                             :migrater (->MockMigrater)
                                             :database (map->MockDatabase
                                                        {:connect-fn identity
                                                         :disconnect-fn identity})})]
            (t/is (= {:ref nil}
                     (select-keys (.stop (.start datasets))
                                  [:ref])))))))))
