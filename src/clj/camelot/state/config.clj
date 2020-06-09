(ns camelot.state.config)

(defn lookup [config k]
  (get config k))

(defn lookup-path [config k]
  (get-in config [:paths k]))
