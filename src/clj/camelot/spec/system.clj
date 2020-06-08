(ns camelot.spec.system
  "System specs."
  (:require
   [clojure.java.io :as io]
   [camelot.market.spec]
   [camelot.state.config :as config]
   [camelot.util.data :as datautil]
   [camelot.testutil.mock :as mock]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.alpha :as s]))

(s/def ::figwheel boolean?)
(s/def ::browser boolean?)
(s/def ::port number?)

;; should use the real things, but currently causes cyclic dep.
(s/def ::config  (s/keys))
(s/def ::database (s/keys :req-un []))
(s/def ::app (s/keys :req-un [::port ::browser] :opt-un [::figwheel]))
(s/def ::jetty (s/keys))
(s/def ::importer (s/keys))

(defn- paths-to-file-objects
  "Transform all values under :paths to `File` objects."
  [m]
  (update m :paths #(datautil/update-vals % io/file)))

(s/def ::non-empty-datasets
  (s/with-gen (s/and map? seq)
    #(gen/fmap (fn [x] (datautil/update-vals x paths-to-file-objects))
               (s/gen :dataset/datasets))))
(s/def ::datasets (s/with-gen map?
                    #(gen/fmap (fn [d] (mock/datasets d (first (keys d))))
                               (s/gen ::non-empty-datasets))))

(s/def ::state
  (s/keys :req-un [::config ::database ::app ::datasets]
          :opt-un [::jetty ::importer ::figwheel]))
