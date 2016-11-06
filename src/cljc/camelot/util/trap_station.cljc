(ns camelot.util.trap-station
  "Utilities for working with trap station data."
  )

(defn valid-range?
  [rs re l]
  (or (nil? l) (and (>= l rs) (<= l re))))

(def valid-longitude? (partial valid-range? -180.0 180.0))
(def valid-latitude? (partial valid-range? -90.0 90.0))
