(ns ctdp.core
  (:require [ctdp.album-reader :as ar]
            [ctdp.config :refer :all]
            [ctdp.problems :as problems]
            [ctdp.translations :refer :all]
            [taoensso.tower :as tower]))

(defn maybe-apply
  [f albsev]
  (do (when (< (problems/severities (:severity albsev)) (problems/severities :warn))
        (f (:album albsev)))
      (:severity albsev)))

(defn run-albums
  [state f albums]
  (let [prob-fn #(printf "[%s] %s: %s\n" %1 %2 %3)
        proc-fn (fn [[dir alb]] (problems/process-problems state prob-fn dir (:problems alb)))
        albsevs (map #(hash-map :album % :severity (proc-fn %)) albums)
        most-sev (reduce problems/highest-severity (map :severity albsevs))]
    (if (= most-sev :error)
      '(:error)
      (map #(maybe-apply f %) albsevs))))

(defn run
  [dir]
  (let [state {:config config
               :translations (tower/make-t tconfig)}
        albums (ar/data-from-tree state (clojure.java.io/file dir))
        album-printer #(clojure.pprint/pprint %)]
    (run-albums state album-printer albums)))
