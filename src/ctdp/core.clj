(ns ctdp.core
  (:require [ctdp.photoset-manager :as pm]
            [ctdp.config :refer :all]
            [cats.monad.either :as either]))

(map (fn [[k v]] (either/branch v
                                (partial printf "[WARNING] %s: %s" k)
                                clojure.pprint/pprint))
 (pm/data-from-tree config (clojure.java.io/file (:rootdir config))))
