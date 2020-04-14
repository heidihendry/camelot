(ns camelot.testutil.db
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [camelot.util.db :as db]))

(s/def ::db-connection (s/keys))

(defmacro with-queries
  [queries & body]
  (let [get-query `(fn [_# scope# qkey#]
                     (get-in ~queries [scope# qkey#]))]
    `(do
       (stest/instrument `db/get-query {:replace {`db/get-query ~get-query}})
       ~@body
       (stest/unstrument `db/get-query))))
