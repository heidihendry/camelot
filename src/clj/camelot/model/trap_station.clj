(ns camelot.model.trap-station
  (:require [schema.core :as s]))

(defn valid-range?
  [rs re l]
  (or (nil? l) (and (>= l rs) (<= l re))))

(def valid-longitude? (partial valid-range? -180.0 180.0))
(def valid-latitude? (partial valid-range? -90.0 90.0))

(def TrapStationCreate
  {:trap-station-name s/Str
   :survey-site-id s/Num
   :trap-station-longitude (s/pred valid-longitude?)
   :trap-station-latitude (s/pred valid-latitude?)
   :trap-station-altitude (s/maybe s/Num)
   :trap-station-sublocation (s/maybe s/Str)
   :trap-station-notes (s/maybe s/Str)})

(def TrapStation
  (merge TrapStationCreate
         {:trap-station-id s/Num
          :trap-station-created org.joda.time.DateTime
          :trap-station-updated org.joda.time.DateTime}))
