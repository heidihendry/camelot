(ns camelot.util.deployment
  (:require [schema.core :as s]))

(s/defn camera-id-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-id")))

(s/defn camera-status-id-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-status-id")))

(s/defn camera-media-unrecoverable-key :- s/Keyword
  [cam-type :- (s/enum :primary :secondary)]
  (keyword (str (name cam-type) "-camera-media-unrecoverable")))
