(ns camelot.state.config
  (:require [camelot.util.data :as datautil]
            [clojure.java.io :as io]
            [camelot.market.config :as market-config]))

(def config-cache (atom nil))

(defn- paths-to-file-objects
  "Transform all values under :paths to `File` objects."
  [m]
  (letfn [(to-file-objects [p] (into {} (map (fn [[k v]] [k (io/file v)]) p)))]
    (update m :paths to-file-objects)))

(defn read-config
  []
  (when (nil? @config-cache)
    (reset! config-cache
            (-> (market-config/read-config)
                (paths-to-file-objects)
                (update :datasets datautil/update-vals paths-to-file-objects))))
  @config-cache)

(defn lookup [config k]
  (get config k))

(defn lookup-path [config k]
  (get-in config [:paths k]))
