(ns ctdp.core
  (:require [ctdp.album-reader :as ar]
            [ctdp.config :refer :all]
            [ctdp.translations :refer :all]
            [cats.monad.either :as either]
            [taoensso.tower :as tower]))

(defn run
  [dir]
  (let [state {:config config
               :translations (tower/make-t tconfig)}
        warn ((:translations state) (:language (:config state)) :error/warn)
        errfn #(printf "[%s] %s: %s\n" warn %1 %2)]
    (map (fn [[k v]] (either/branch v (partial errfn k) clojure.pprint/pprint))
         (ar/data-from-tree state (clojure.java.io/file dir)))))
