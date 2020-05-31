(ns camelot.state.config
  (:require [camelot.state.util :as util]
            [camelot.market.config :as market-config]))

(def config-cache (atom nil))

(defn read-config
  []
  (when (nil? @config-cache)
    (reset! config-cache
            (util/paths-to-file-objects (market-config/read-config))))
  @config-cache)

(defn lookup [config k]
  (get config k))

(defn lookup-path [config k]
  (get-in config [:paths k]))
