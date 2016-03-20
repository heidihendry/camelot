(ns ctdp.core
  (:require [ctdp.reader.dirtree :as r]
            [ctdp.album :as a]
            [ctdp.config :refer [state]]
            [ctdp.problems :as problems]))

(defn maybe-apply
  [f albsev]
  (do
    (when (< (problems/severities (:severity albsev)) (problems/severities :warn))
      (f (:album albsev)))
    (:severity albsev)))

(defn run-albums
  [state f albums]
  (let [prob-fn #(printf "[%s] %s: %s\n" %1 %2 %3)
        proc-fn (fn [[dir alb]] (problems/process-problems state prob-fn dir (:problems alb)))
        albsevs (map #(hash-map :album % :severity (proc-fn %)) albums)
        most-sev (reduce problems/highest-severity :okay (map :severity albsevs))]
    (if (= most-sev :error)
      albsevs
      (map #(maybe-apply f %) albsevs))))

(defn run
  [dir]
  (let [album-fn #(clojure.pprint/pprint %)]
    (->> dir
         (r/read-tree state)
         (a/album-set state)
         (run-albums state album-fn))))
