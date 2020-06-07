(ns camelot.spec.system
  "System specs."
  (:require
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

(s/def ::non-empty-datasets
  (s/with-gen (s/and map? seq)
    #(gen/fmap (fn [x] (datautil/update-vals x config/paths-to-file-objects))
               (s/gen :dataset/datasets))))
(s/def ::datasets (s/with-gen map?
                    #(gen/fmap mock/datasets (s/gen ::non-empty-datasets))))

(s/def ::state
  (s/keys :req-un [::config ::database ::app ::datasets]
          :opt-un [::jetty ::importer ::figwheel]))
