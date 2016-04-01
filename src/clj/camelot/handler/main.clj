(ns camelot.handler.main
  (:require [camelot.reader.dirtree :as r]
            [camelot.album :as a]
            [camelot.config :refer [gen-state config]]
            [camelot.problems :as problems]
            [camelot.action.rename-photo :as rp]
            [camelot.util.java-file :as f]))

(def cache
  "The contents of the album set and metadata"
  (atom {}))

(defn maybe-apply
  "Apply function `f` to album, so long as there aren't any warnings'"
  [f albsev]
  (do
    (when (< (problems/severities (:severity albsev)) (problems/severities :warn))
      (f (:album albsev)))
    (:album albsev)))

(defn run-albums
  "Run `maybe-apply` across all albums, if no album poses an error."
  [state f albums]
  (let [prob-fn #(printf "%s%s: %s\n" %1 %2 %3)
        proc-fn (fn [[dir alb]] (problems/process-problems state prob-fn dir (:problems alb)))
        albsevs (map #(hash-map :album % :severity (proc-fn %)) albums)
        most-sev (reduce problems/highest-severity :okay (map :severity albsevs))]
    (if (= most-sev :error)
      albsevs
      (into {} (map #(maybe-apply f %) albsevs)))))

(defn read-albums
  "Read albums if not already cached.
Otherwise return the contents of the cache."
  [state dir]
  (->> dir
       (r/read-tree state)
       (a/album-set state)))

(defn run-tests
  [state acc [file alb]]
  (assoc acc file (a/list-problems state alb)))

(defn consistency-check
  "Check album consistency"
  [state albums]
  (println ((:translate state) :checks/starting))
  (run! println (map (fn [[k v]]
                       (str ((:translate state) :checks/failure-notice (f/getPath k))
                            "\n  * "
                            (clojure.string/join
                             "\n  * "
                             (map #((:translate state)
                                    (keyword (str "checks/" (name %))))
                                  (seq v)))))
                     (reduce (partial run-tests state) {} albums))))

(defn run
  "Retrieve album data and apply transformations."
  [dir]
  (let [state (gen-state config)
        album-transform #(->> %
                              (rp/rename-photos state))]
    (->> dir
         (read-albums state)
         (run-albums state album-transform)
         (reset! cache))))
