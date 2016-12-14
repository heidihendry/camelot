(ns camelot.util.config)

(defn lookup [state k]
  (let [store (get-in state [:config :store])]
    (let [sv @store]
      (get (merge sv (or (:session state) {})) k))))
